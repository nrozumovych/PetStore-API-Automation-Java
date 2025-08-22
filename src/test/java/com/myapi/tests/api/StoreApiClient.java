package com.myapi.tests.api;

import com.myapi.tests.models.Order;
import com.myapi.tests.specs.ApiSpecifications;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

/**
 * API client for interacting with the /store endpoints of the Pet Store API.
 */
public class StoreApiClient {

    /**
     * Sends a POST request to place a new order.
     * This method expects a 200 OK response and applies standard response specifications.
     *
     * @param order The {@link Order} object to place.
     * @return Response object from Rest Assured, with 200 OK status implicitly asserted.
     */
    public Response placeOrder(Order order){
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .body(order)
                .log().all()
                .when()
                .post("/store/order")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .extract().response();
    }

    /**
     * Sends a GET request to retrieve an order by its ID.
     * Returns a raw response, which can have any status code (200, 404, etc.).
     *
     * @param orderId The unique identifier of the order.
     * @return The Response object from Rest Assured.
     */
    public Response getOrderByIdRawResponse(Integer orderId){
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .pathParam("orderId", orderId)
                .log().all()
                .when()
                .get("/store/order/{orderId}")
                .then()
                .log().all()
                .extract().response();

    }

    /**
     * Sends a GET request to retrieve an order by its ID and waits until the response
     * reflects the expected petId. This is useful for testing delayed updates.
     *
     * @param orderId The unique identifier of the order to retrieve.
     * @param expectedPetId The petId expected in the response body after the update.
     * @return The Response object from Rest Assured once the condition is met.
     */
    public Response getOrderByIdAndVerifyPetId(Integer orderId, Integer expectedPetId) {
        AtomicReference<Response> finalResponse = new AtomicReference<>();
        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    Response response = getOrderByIdRawResponse(orderId);
                    if (response.statusCode() == HttpStatus.SC_OK && response.jsonPath().getInt("petId") == expectedPetId) {
                        finalResponse.set(response);
                        return true;
                    }
                    return false;
                });

        return finalResponse.get();
    }

    /**
     * Sends a DELETE request to remove a specific order by its ID.
     *
     * @param orderId The unique identifier of the order to delete.
     * @return The Response object from Rest Assured.
     */
    public Response deleteOrder(Integer orderId){
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .pathParam("orderId", orderId)
                .log().all()
                .when()
                .delete("/store/order/{orderId}")
                .then()
                .log().all()
                .extract().response();
    }

    /**
     * Sends a GET request to retrieve pet inventory counts by status.
     * Expects a 200 OK response and applies standard response specifications.
     *
     * @return Response object from Rest Assured, with 200 OK status implicitly asserted.
     */
    public Response getInventoryByStatus(){
        return RestAssured.given()
                .spec(ApiSpecifications.requestSpec)
                .log().all()
                .when()
                .get("/store/inventory")
                .then()
                .log().all()
                .spec(ApiSpecifications.responseSpec200)
                .extract().response();
    }
    /**
     * Sends a GET request to retrieve inventory and waits for a specific status count
     * to match the expected value. This is useful for handling eventual consistency.
     *
     * @param status The status of the pet to check.
     * @param expectedCount The expected count for the specified status.
     */
    public void getInventoryAndVerifyStatusCount(String status, int expectedCount) {
        AtomicReference<Response> finalResponse = new AtomicReference<>();
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Response response = getInventoryByStatus();
                    response.then()
                            .statusCode(HttpStatus.SC_OK)
                            .body(status, equalTo(expectedCount));
                    finalResponse.set(response);
                });
        finalResponse.get();
    }
}
