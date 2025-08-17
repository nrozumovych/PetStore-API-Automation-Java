package com.myapi.tests.tests;

import com.myapi.tests.api.UserApiClient;
import com.myapi.tests.models.User;
import com.myapi.tests.specs.ApiSpecifications;
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
    public void createUserAndGetUserTest() {
        ApiSpecifications.initSpecs();
        this.userApiClient = new UserApiClient();
        User newUser = createRandomUser();

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
    public void updateUserTest() {
        User userToUpdate = createRandomUser();

        userApiClient.getUserByUsername(userToUpdate.getUsername());

        String updatedFirstName = "Kate";
        String updatedEmail = "updated_" + System.currentTimeMillis() + "@example.com";
        String updatedPhone = "987-654-3210";

        userToUpdate.setFirstName(updatedFirstName);
        userToUpdate.setEmail(updatedEmail);
        userToUpdate.setPhone(updatedPhone);

        Response updateResponse = userApiClient.updateUser(userToUpdate);

        updateResponse.then()
                .statusCode(200);

        Response getResponse = userApiClient.waitForUserUpdate(userToUpdate.getUsername(),
                updatedFirstName, 10);

        getResponse.then()
                .body("firstName", equalTo(updatedFirstName))
                .body("email", equalTo(updatedEmail))
                .body("phone", equalTo(updatedPhone));

        System.out.println("Update User Test Passed for username: " + userToUpdate.getUsername());
    }

    @Test(description = "Verify user deletion functionality", groups = {"SMOKE"})
    public void deleteUserTest() {
        User userToDelete = createRandomUser();
        userApiClient.getUserByUsername(userToDelete.getUsername());
        userApiClient.deleteUser(userToDelete.getUsername());

        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    Response response = userApiClient.getUserByUsernameRawResponse(userToDelete.getUsername());
                    if (response.statusCode() == HttpStatus.SC_NOT_FOUND) {
                        return true;
                    } else {
                        throw new AssertionError("User was not deleted from the database. Status code was: " + response.statusCode());
                    }
                });

        System.out.println("Delete User Test Passed for username: " + userToDelete.getUsername());
    }

    @Test(description = "Verify retrieving a non-existent user returns 404 Not Found", groups = {"REGRESSION"})
    public void getNonExistentUserTest() {
        String nonExistentUsername = "nonexistent_user_" + System.currentTimeMillis();

        Response response = userApiClient.getUserByUsernameRawResponse(nonExistentUsername);

        response.then().statusCode(HttpStatus.SC_NOT_FOUND)
                .body("message", equalTo("User not found"));

        System.out.println("getNonExistentUserTest Passed. Response status: " + response.statusCode());
    }

    @Test(description = "Verify deleting a non-existent user returns 404 Not Found", groups = {"REGRESSION"})
    public void deleteNonExistentUserTest() {
        String nonExistentUsername = "nonexistent_user_" + System.currentTimeMillis();

        Response response = userApiClient.deleteUser(nonExistentUsername);

        response.then().statusCode(HttpStatus.SC_NOT_FOUND);

        System.out.println("deleteNonExistentUserTest Passed. Response status: " + response.statusCode());
    }

    @Test(description = "Verify user login and logout functionality", groups = {"SMOKE"})
    public void loginAndLogoutTest() {
        User userForLogin = createRandomUser();

        userApiClient.loginUser(userForLogin.getUsername(), userForLogin.getPassword());

        userApiClient.logoutUser();

        System.out.println("Login and Logout Test Passed for username: " + userForLogin.getUsername());
    }

    @Test(description = "Verify creating multiple users with an array of objects", groups = {"REGRESSION"})
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