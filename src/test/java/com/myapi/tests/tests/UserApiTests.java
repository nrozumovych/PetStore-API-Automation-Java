package com.myapi.tests.tests;

import com.myapi.tests.api.UserApiClient;
import com.myapi.tests.models.User;
import com.myapi.tests.specs.ApiSpecifications;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

public class UserApiTests extends BaseTest {
    protected UserApiClient userApiClient;
    protected final List<String> createdUserNames = new ArrayList<>();

    @BeforeMethod(alwaysRun = true)
    public void setupUserClient() {
        System.out.println("Initializing UserApiClient...");
        this.userApiClient = new UserApiClient();
    }

    protected User createRandomUser() {
        User user = generateRandomUserObject();

        Response createResponse = userApiClient.createUser(user);
        createResponse
                .then()
                .spec(ApiSpecifications.responseSpec200)
                .body("message", equalTo(String.valueOf(user.getId())));

        this.createdUserNames.add(user.getUsername());
        return user;

    }

    protected User generateRandomUserObject() {
        String uniqueUserName = "testuser_" + System.currentTimeMillis();
        String uniqueEmail = "email_" + System.currentTimeMillis() + "@test.com";
        Integer uniqueId = new Random().nextInt(1_000_000_000) + 1_000_000;

        return User.builder()
                .id(uniqueId)
                .username(uniqueUserName)
                .firstName("John")
                .lastName("Doe")
                .email(uniqueEmail)
                .password("password123")
                .phone("123-456-7890")
                .userStatus(1)
                .build();
    }

    @AfterMethod
    public void cleanUpUsers() {
        for (String username : createdUserNames) {
            System.out.println("Cleaning up user with username: " + username);
            try {
                userApiClient.deleteUser(username)
                        .then()
                        .statusCode(anyOf(equalTo(200), equalTo(404)));
            } catch (Exception e) {
                System.err.println("Failed to delete user " + username + ": " + e.getMessage());
            }
        }
        createdUserNames.clear();
    }

    @Test(description = "Verify user creation and retrieval", groups = {"SMOKE"})
    @Description("This test covers the user creation and retrieval process. It creates a new user with random data, retrieves it by username, and then validates that all user details match the data used for creation.")
    public void createUserAndGetUserTest() {
        User newUser = createUserAndVerifySuccess();
        verifyRetrievedUser(newUser);
    }

    @Step("1. Create a new user and verify success")
    private User createUserAndVerifySuccess() {
        return createRandomUser();
    }

    @Step("2. Retrieve user by username and verify details")
    private void verifyRetrievedUser(User newUser) {
        Response getResponse = userApiClient.getUserByUsername(newUser.getUsername());

        getResponse.then()
                .body("id", equalTo(newUser.getId()))
                .body("username", equalTo(newUser.getUsername()))
                .body("firstName", equalTo(newUser.getFirstName()))
                .body("email", equalTo(newUser.getEmail()));

        User retrievedUser = getResponse.as(User.class);

        Assert.assertEquals(retrievedUser.getLastName(), newUser.getLastName(), "Last name mismatch");
        Assert.assertEquals(retrievedUser.getPhone(), newUser.getPhone(), "Phone mismatch");
        Assert.assertEquals(retrievedUser.getUserStatus(), newUser.getUserStatus(), "User status mismatch");

        System.out.println("Create and Get User Test Passed for username: " + newUser.getUsername());
    }

    @Test(description = "Verify user update functionality", groups = {"SMOKE"})
    @Description("This test validates the user update functionality. It creates a user, updates several of its fields (first name, email, phone), and then verifies that the changes are correctly applied and can be retrieved from the API.")
    public void updateUserTest() {
        User userToUpdate = createRandomUser();
        updateAndVerifyUser(userToUpdate);
        waitForAndVerifyUpdate(userToUpdate);
    }

    @Step("1. Update user fields and verify update response")
    private void updateAndVerifyUser(User userToUpdate) {
        String updatedFirstName = "Kate";
        String updatedEmail = "updated_" + System.currentTimeMillis() + "@example.com";
        String updatedPhone = "987-654-3210";

        userToUpdate.setFirstName(updatedFirstName);
        userToUpdate.setEmail(updatedEmail);
        userToUpdate.setPhone(updatedPhone);

        Response updateResponse = userApiClient.updateUser(userToUpdate);
        updateResponse.then().statusCode(200);
    }

    @Step("2. Wait for and verify the updated user details")
    private void waitForAndVerifyUpdate(User userToUpdate) {
        Response getResponse = userApiClient.waitForUserUpdate(userToUpdate.getUsername(),
                userToUpdate.getFirstName(), 10);

        getResponse.then()
                .body("firstName", equalTo(userToUpdate.getFirstName()))
                .body("email", equalTo(userToUpdate.getEmail()))
                .body("phone", equalTo(userToUpdate.getPhone()));

        System.out.println("Update User Test Passed for username: " + userToUpdate.getUsername());
    }

    @Test(description = "Verify user deletion functionality", groups = {"SMOKE"})
    @Description("This test verifies that a user can be successfully deleted from the system. It creates a new user, deletes it, and then confirms that the user no longer exists by checking for a 404 Not Found response.")
    public void deleteUserTest() {
        User userToDelete = createRandomUser();
        deleteUserAndVerify(userToDelete.getUsername());
    }

    @Step("Delete user and confirm deletion")
    private void deleteUserAndVerify(String username) {
        userApiClient.getUserByUsername(username);
        userApiClient.deleteUser(username);

        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    Response response = userApiClient.getUserByUsernameRawResponse(username);
                    return response.statusCode() == HttpStatus.SC_NOT_FOUND;
                });
        System.out.println("Delete User Test Passed for username: " + username);
    }

    @Test(description = "Verify retrieving a non-existent user returns 404 Not Found", groups = {"REGRESSION"})
    @Description("This test confirms the API's behavior for an invalid request. It attempts to retrieve a user with a non-existent username and validates that the service responds with the correct HTTP 404 Not Found status code and a 'User not found' message.")
    public void getNonExistentUserTest() {
        String nonExistentUsername = "nonexistent_user_" + System.currentTimeMillis();

        Response response = userApiClient.getUserByUsernameRawResponse(nonExistentUsername);

        response.then().statusCode(HttpStatus.SC_NOT_FOUND)
                .body("message", equalTo("User not found"));

        System.out.println("getNonExistentUserTest Passed. Response status: " + response.statusCode());
    }

    @Test(description = "Verify deleting a non-existent user returns 404 Not Found", groups = {"REGRESSION"})
    @Description("This test verifies the API's behavior when a delete request is sent for a user that does not exist. It expects a 404 Not Found status code.")
    public void deleteNonExistentUserTest() {
        String nonExistentUsername = "nonexistent_user_" + System.currentTimeMillis();
        Response response = userApiClient.deleteUser(nonExistentUsername);

        response.then().statusCode(HttpStatus.SC_NOT_FOUND);

        System.out.println("deleteNonExistentUserTest Passed. Response status: " + response.statusCode());
    }

    @Test(description = "Verify user login and logout functionality", groups = {"SMOKE"})
    @Description("This test validates the login and logout endpoints. It creates a user, logs in using its credentials, and then logs out, ensuring the API responds correctly at each step.")
    public void loginAndLogoutTest() {
        User userForLogin = createRandomUser();
        userApiClient.loginUser(userForLogin.getUsername(), userForLogin.getPassword());
        userApiClient.logoutUser();
        System.out.println("Login and Logout Test Passed for username: " + userForLogin.getUsername());
    }

    @Test(description = "Verify creating multiple users with an array of objects", groups = {"REGRESSION"})
    @Description("This test validates the batch user creation endpoint by creating a new array of user objects and sending a single request. It then verifies that all users were successfully created by retrieving them individually.")
    public void createUsersWithArrayTest() {
        User[] usersArray = {
                generateRandomUserObject(),
                generateRandomUserObject(),
                generateRandomUserObject()
        };
        for (User user : usersArray) {
            createdUserNames.add(user.getUsername());
        }

        userApiClient.createUsersWithArray(usersArray);

        for (User user : usersArray) {
            userApiClient.getUserByUsername(user.getUsername());
        }

        System.out.println("createUsersWithArrayTest passed successfully.");
    }

    @Test(description = "Verify creating multiple users with a list of objects", groups = {"REGRESSION"})
    @Description("This test validates the batch user creation endpoint by creating a new list of user objects and sending a single request. It then verifies that all users were successfully created by retrieving them individually.")
    public void createUsersWithListTest() {
        List<User> usersList = new ArrayList<>();
        usersList.add(generateRandomUserObject());
        usersList.add(generateRandomUserObject());
        usersList.add(generateRandomUserObject());

        for (User user : usersList) {
            createdUserNames.add(user.getUsername());
        }

        userApiClient.createUsersWithList(usersList);

        for (User user : usersList) {
            userApiClient.getUserByUsername(user.getUsername());
        }

        System.out.println("createUsersWithListTest passed successfully.");
    }
}