package com.myapi.tests.api;

import com.myapi.tests.models.Pet;
import com.myapi.tests.specs.ApiSpecifications;
import io.restassured.response.Response;
import io.restassured.RestAssured;
import org.apache.http.HttpStatus;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * API client for interacting with the /pet endpoints of the Pet Store API.
 */
public class PetApiClient {
    /**
     * Sends a POST request to add a new pet.
     *
     * @param pet The Pet object to add.
     * @return Response object from Rest Assured.
     */
    public Response addPet(Pet pet) {
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .body(pet)
                .log().all()
                .when()
                .post("/pet")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .extract().response();
    }

    /**
     * Sends a raw GET request to retrieve a pet by ID.
     * This method does not include retry logic and is used by the
     * getPetById() method to perform repeated attempts.
     *
     * @param petId The ID of the pet to be retrieved.
     * @return The API response as a Response object.
     */
    public Response getPetByIdRawResponse(Integer petId) {
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .pathParam("petId", petId)
                .log().all()
                .when()
                .get("/pet/{petId}")
                .then()
                .log().all()
                .extract().response();
    }
    /**
     * Sends a raw GET request to retrieve a pet by ID.
     * This method is specifically for pets with large IDs and does not include retry logic.
     *
     * @param petId The ID of the pet to be retrieved.
     * @return The API response as a Response object.
     */
    public Response getPetByIdRawResponse(Long petId) {
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .pathParam("petId", petId)
                .log().all()
                .when()
                .get("/pet/{petId}")
                .then()
                .log().all()
                .extract().response();
    }

    /**
     * Sends a GET request to retrieve a pet by its ID, waiting for the resource to become available.
     * This method wraps the raw request with retry logic to ensure test stability.
     *
     * @param petId The ID of the pet to be retrieved.
     * @return The API response with status code 200.
     */
    public Response getPetById(Integer petId) {
        return await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> getPetByIdRawResponse(petId),
                        res -> res.statusCode() == HttpStatus.SC_OK);
    }

    /**
     * Sends a GET request to verify a resource has been successfully deleted.
     * This method waits for the API to return a 404 Not Found status code,
     * handling any temporary latency after a deletion.
     *
     * @param petId The ID of the pet to be verified as deleted.
     * @return The API response with status code 404.
     */
    public Response getPetByIdExpectingError(Integer petId) {
        return await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> getPetByIdRawResponse(petId),
                        res -> res.statusCode() == HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Sends a PUT request to update an existing pet.
     *
     * @param pet The Pet object with the updated data.
     * @return Response object from Rest Assured.
     */
    public Response updatePet(Pet pet) {
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .body(pet)
                .log().all()
                .when()
                .put("/pet")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .extract().response();
    }

    /**
     * Sends a DELETE request to delete a pet by its ID, waiting for the API to confirm the deletion.
     * This method uses a retry mechanism to correctly handle cases where
     * the pet is either successfully deleted (200 OK).
     *
     * @param petId The ID of the pet to be deleted.
     * @return The API response with a status code of 200 or 404.
     */
    public Response deletePet(Integer petId) {
        return await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> deletePetRawResponse(petId),
                        res -> res.statusCode() == HttpStatus.SC_OK);
    }

    /**
     * Sends a raw DELETE request to delete a pet by ID.
     * This method does not include retry logic and is used by the
     * deletePet() and cleanup methods to perform the raw deletion.
     *
     * @param petId The ID of the pet to be deleted.
     * @return The API response as a Response object.
     */
    public Response deletePetRawResponse(Integer petId) {
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .pathParam("petId", petId)
                .log().all()
                .when()
                .delete("/pet/{petId}")
                .then()
                .log().all()
                .extract().response();
    }

    /**
     * Sends a GET request to get pets by status.
     * This method verifies that the service is running and returns a 200 OK response.
     *
     * @param status The status of the pets (e.g., "available", "pending", "sold").
     * @return Response object from Rest Assured.
     */
    public Response getPetsByStatus(String status) {
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .queryParam("status", status)
                .log().all()
                .when()
                .get("/pet/findByStatus")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .extract().response();
    }
}