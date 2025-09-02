package com.myapi.tests.tests;

import com.myapi.tests.api.PetApiClient;
import com.myapi.tests.api.StoreApiClient;
import com.myapi.tests.models.Category;
import com.myapi.tests.models.Order;
import com.myapi.tests.models.Pet;
import com.myapi.tests.models.Tag;
import com.myapi.tests.specs.ApiSpecifications;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.Matchers.equalTo;

public class StoreApiTests extends BaseTest {
    protected PetApiClient petApiClient;
    protected StoreApiClient storeApiClient;
    private static final Logger logger = LoggerFactory.getLogger(StoreApiTests.class);

    @BeforeMethod(alwaysRun = true)
    public void setupClients(){
        this.petApiClient =  new PetApiClient();
        this.storeApiClient =  new StoreApiClient();
    }

    @Test(description = "Verify that a new order can be successfully placed for an existing pet", groups = {"REGRESSION"})
    @Description("This end-to-end test covers the full lifecycle of an order: place, retrieve, and delete. It ensures the system correctly creates, reads, and deletes an order for an existing pet, validating data integrity at each stage.")
    public void e2eTestCreateReadDeleteOrderForExistingPetTest(){
        Integer petId = createNewPet("available");
        Integer orderId = placeOrderStep(petId);
        retrieveOrderStep(orderId, petId);
        deleteOrderStep(orderId);
    }
    @Step("1. Place a new order for pet with ID: {0}")
    private Integer placeOrderStep(Integer petId) {
        Integer orderId = new Random().nextInt(999) + 1;
        Order newOrder = getOrder(petId, orderId);

        Response orderResponse = storeApiClient.placeOrder(newOrder);
        orderResponse.then()
                .statusCode(HttpStatus.SC_OK)
                .body("id",equalTo(newOrder.getId()))
                .body("petId", equalTo(petId))
                .body("status", equalTo(newOrder.getStatus()))
                .body("quantity", equalTo(newOrder.getQuantity()))
                .body("shipDate", equalTo(newOrder.getShipDate()))
                .body("complete", equalTo(newOrder.getComplete()));

        System.out.println("Test finished. Order successfully placed with ID: " + orderId);
        return orderId;
    }
    @Step("2. Retrieve an order with ID: {0} and verify pet ID: {1}")
    private void retrieveOrderStep(Integer orderId, Integer petId) {
        Response response = storeApiClient.getOrderByIdAndVerifyPetId(orderId, petId);
        response.then()
                .statusCode(HttpStatus.SC_OK)
                .body("id", equalTo(orderId))
                .body("petId", equalTo(petId));
        System.out.println("Test finished. The order was successfully retrieved and validated.");
    }

    @Step("3. Delete an order with ID: {0}")
    private void deleteOrderStep(Integer orderId) {
        Response deleteResponse = storeApiClient.deleteOrder(orderId);
        deleteResponse.then()
                .statusCode(HttpStatus.SC_OK)
                .body("code", equalTo(200))
                .body("message", equalTo(String.valueOf(orderId)));
        System.out.println("Test finished. The order was successfully deleted.");
    }

    @Test(description = "Verify that a request for a non-existent order returns a 404 Not Found", groups = {"REGRESSION"})
    @Description("This test confirms the API's behavior for an invalid request. It attempts to retrieve an order with a non-existent ID and validates that the service responds with the correct HTTP 404 Not Found status code and a 'Order not found' message.")
    public void retrieveNonExistentOrderTest() {
        Integer nonExistentOrderId = -1;

        Response response = storeApiClient.getOrderByIdRawResponse(nonExistentOrderId);
        response.then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("code", equalTo(1))
                .body("type", equalTo("error"))
                .body("message", equalTo("Order not found"));

        logger.info("Test finished. The API correctly returned a 404 for a non-existent order.");
    }
    @Test(description = "Verify that a request for deleting a non-existent order returns a 404 Not Found", groups = {"REGRESSION"})
    @Description("This test verifies the API's behavior when a delete request is sent for an order that does not exist. It expects a 404 Not Found status code and an appropriate error message.")
    public void deleteNonExistentOrderTest() {
        Integer nonExistentOrderId = -1;

        Response response = storeApiClient.deleteOrder(nonExistentOrderId);
        response.then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("code", equalTo(404))
                .body("type", equalTo("unknown"))
                .body("message", equalTo("Order Not Found"));

        logger.info("Test finished. The API correctly returned a 404 for a non-existent order.");
    }
    @Test(description = "Verify that the inventory count for available pets increments correctly after adding a new pet", groups = {"REGRESSION"})
    @Description("This test validates the `store/inventory` endpoint. It gets the initial inventory count, creates a new pet, and then verifies that the inventory count for that pet's status has been correctly incremented.")
    public void getInventoryByStatusTest() {
        logger.info("Step 1: Get initial inventory count for 'available' pets.");
        Response initialResponse = storeApiClient.getInventoryByStatus();
        String uniqueStatus = "test-status-" + System.currentTimeMillis();
        int initialAvailableCount = initialResponse.jsonPath().get(uniqueStatus) != null ? initialResponse.jsonPath().getInt(uniqueStatus) : 0;

        logger.info("Step 2: Create a new pet with 'available' status.");
        Integer petId = createNewPet(uniqueStatus);

        logger.info("Step 3: Get updated inventory count.");
        storeApiClient.getInventoryAndVerifyStatusCount(uniqueStatus, initialAvailableCount + 1);

        System.out.println("Test finished. The inventory count for '" + uniqueStatus + "' pets was successfully validated.");
        petApiClient.deletePet(petId);
    }

    private Order getOrder(Integer petId, Integer orderId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String shipDate = sdf.format(new Date());
        return Order.builder()
                .id(orderId)
                .petId(petId)
                .quantity(1)
                .shipDate(shipDate)
                .status("placed")
                .complete(false)
                .build();
    }

    private Integer createNewPet(String status) {
        Integer petId = new Random().nextInt(99_000_000) + 1_000_000;

        List<Tag> initialTags = new ArrayList<>(Arrays.asList(
                Tag.builder().id(10).name("Friendly").build(),
                Tag.builder().id(11).name("Cute").build()
        ));

        Pet pet = Pet.builder()
                .id(petId)
                .name("TestPet_" + System.currentTimeMillis())
                .category(Category.builder().id(1).name("Dogs").build())
                .photoUrls(Arrays.asList("http://example.com/photo1.jpg", "http://example.com/photo2.jpg"))
                .tags(initialTags)
                .status(status)
                .build();

        Response createResponse = petApiClient.addPet(pet);
        createResponse.then().spec(ApiSpecifications.responseSpec200);
        return petId;
    }
}
