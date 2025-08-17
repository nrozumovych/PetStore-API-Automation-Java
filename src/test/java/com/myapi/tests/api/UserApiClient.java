package com.myapi.tests.api;

import com.myapi.tests.models.User;
import com.myapi.tests.specs.ApiSpecifications;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * API client for interacting with the /user endpoints of the Pet Store API.
 */
public class UserApiClient {
    /**
     * Sends a POST request to create a new user.
     * Expects a 200 OK response and applies standard response specifications.
     *
     * @param user The User object to create.
     * @return Response object from Rest Assured, with 200 OK status implicitly asserted.
     */
    public Response createUser(User user) {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .body(user)
                .log().all()
                .when()
                .post("/user")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .extract().response();
    }

    /**
     * Sends a GET request to retrieve a user by their username.
     * No assertions are performed on the response status code.
     *
     * @param username The username of the user to get.
     * @return The raw Response object from Rest Assured.
     */
    public Response getUserByUsernameRawResponse(String username) {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .pathParam("username", username)
                .log().all()
                .when()
                .get("/user/{username}")
                .then()
                .log().all()
                .extract().response();
    }

    /**
     * Sends a GET request to retrieve a user by their username.
     * Expects a 200 OK response and applies standard response specifications.
     *
     * @param username The username of the user to retrieve.
     * @return Response object from Rest Assured, with 200 OK status implicitly asserted.
     */
    public Response getUserByUsername(String username) {
        Response response = await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> getUserByUsernameRawResponse(username),
                        res -> res.statusCode() == HttpStatus.SC_OK);

        if (response.statusCode() != HttpStatus.SC_OK) {
            throw new AssertionError("User with username " + username + " was not found after waiting. " +
                    "Actual status code was: " + response.statusCode());
        }
        return response;
    }

    /**
     * Sends a raw PUT request to update an existing user.
     * This method does not include retry logic.
     *
     * @param user The User object with updated details.
     * @return The API response as a Response object.
     */
    public Response updateUserRaw(User user) {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .pathParam("username", user.getUsername())
                .body(user)
                .log().all()
                .when()
                .put("/user/{username}")
                .then()
                .log().all() // <-- Добавляем логгирование ответа
                .extract().response();
    }

    /**
     * Sends a PUT request to update a user, waiting for the API to confirm the change.
     * This method wraps the raw request with retry logic.
     *
     * @param user The User object with updated details.
     * @return The API response with status code 200.
     */
    public Response updateUser(User user) {
        return await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> updateUserRaw(user),
                        res -> res.statusCode() == HttpStatus.SC_OK);
    }

    /**
     * Sends a DELETE request to delete a user by their username.
     * Returns the raw Response object without automatically asserting a 200 OK status.
     * This method is useful for cleanup operations or negative test scenarios
     * where various status codes (e.g., 200, 404) might be expected.
     *
     * @param username The username of the user to delete.
     * @return Raw Response object from Rest Assured, allowing for flexible status code assertions.
     */
    public Response deleteUser(String username) {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .pathParam("username", username)
                .log().all()
                .when()
                .delete("/user/{username}")
                .then()
                .log().all()
                .extract().response();
    }

    /**
     * Sends a GET request to log in a user with the provided username and password.
     * Expects a 200 OK response.
     *
     * @param username The username of the user.
     * @param password The password of the user.
     * @return The Response object from the login request.
     */
    public Response loginUser(String username, String password) {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .queryParam("username", username)
                .queryParam("password", password)
                .log().all()
                .when()
                .get("/user/login")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .body("message", startsWith("logged in user session:"))
                .extract().response();
    }
    /**
     * Sends a GET request to log in a user and asserts a specific status code.
     * This method is used for negative testing scenarios, such as missing parameters.
     *
     * @param username The username for the login attempt.
     * @param password The password for the login attempt.
     * @param expectedStatusCode The expected HTTP status code of the response.
     * @return The Response object from the login request.
     */
    public Response loginUserExpectingError(String username, String password, int expectedStatusCode) {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .queryParam("username", username)
                .queryParam("password", password)
                .log().all()
                .when()
                .get("/user/login")
                .then()
                .log().all()
                .statusCode(expectedStatusCode)
                .extract().response();
    }

    /**
     * Sends a GET request to log out a user.
     *
     * @return Response object from Rest Assured.
     */
    public Response logoutUser() {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .log().all()
                .when()
                .get("/user/logout")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .body("message", equalTo("ok"))
                .extract().response();
    }

    /**
     * Sends a POST request to create multiple users with a list of User objects.
     * Expects a 200 OK response and applies standard response specifications.
     *
     * @param users A List of User objects to create.
     * @return Response object from Rest Assured, with 200 OK status implicitly asserted.
     */
    public Response createUsersWithList(List<User> users) {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .body(users)
                .log().all()
                .when()
                .post("/user/createWithList")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .extract().response();
    }

    /**
     * Sends a POST request to create multiple users with an array of User objects.
     * Expects a 200 OK response and applies standard response specifications.
     * (Functionally similar to createUsersWithList for JSON API).
     *
     * @param users An array of User objects to create.
     * @return Response object from Rest Assured, with 200 OK status implicitly asserted.
     */
    public Response createUsersWithArray(User[] users) {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .body(users)
                .log().all()
                .when()
                .post("/user/createWithArray")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .extract().response();
    }

    /**
     * Sends a POST request to create a user and returns the raw response,
     * without asserting a 200 OK status. This is useful for negative test scenarios.
     *
     * @param user The user object to send in the request body.
     * @return Response object from Rest Assured.
     */
    public Response createUserExpectingError(User user) {
        return given()
                .spec(ApiSpecifications.requestSpec)
                .body(user)
                .log().all()
                .when()
                .post("/user")
                .then()
                .log().all()
                .extract().response();
    }

    /**
     * Sends a GET request to a user and waits until the response contains the updated data.
     * This method repeatedly checks the API until the status code is 200 OK
     * and the 'firstName' field matches the expected value.
     *
     * @param username The username of the user to verify.
     * @param expectedFirstName The expected first name after the update.
     * @param timeoutSeconds The maximum time to wait in seconds.
     * @return The API response with a 200 OK status and updated data.
     */
    public Response waitForUserUpdate(String username, String expectedFirstName, long timeoutSeconds) {
        return await()
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> given()
                                .spec(ApiSpecifications.requestSpec)
                                .pathParam("username", username)
                                .log().all()
                                .when()
                                .get("/user/{username}")
                                .then()
                                .log().all()
                                .extract().response(),
                        response -> response.statusCode() == 200 &&
                                response.body().jsonPath().getString("firstName").equals(expectedFirstName));
    }
}
