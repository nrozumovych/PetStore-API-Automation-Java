package com.myapi.tests.tests;

import com.myapi.tests.api.PetApiClient;
import com.myapi.tests.api.StoreApiClient;
import com.myapi.tests.models.Category;
import com.myapi.tests.models.Order;
import com.myapi.tests.models.Pet;
import com.myapi.tests.models.Tag;
import com.myapi.tests.specs.ApiSpecifications;
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
    public void e2eTestCreateReadDeleteOrderForExistingPetTest(){
        logger.info("Preconditions: Creating a new pet");
        Integer petId = createNewPet("available");

        logger.info("Place a new order for pet with ID: {}", petId);
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

        logger.info("Retrieve an order with ID: {}", orderId);

        Response response = storeApiClient.getOrderByIdAndVerifyPetId(orderId, petId);

        response.then()
                .statusCode(HttpStatus.SC_OK)
                .body("id", equalTo(orderId))
                .body("petId", equalTo(petId))
                .body("quantity", equalTo(newOrder.getQuantity()))
                .body("status",  equalTo(newOrder.getStatus()))
                .body("complete", equalTo(newOrder.getComplete()))
                .body("shipDate", equalTo(newOrder.getShipDate()));


        System.out.println("Test finished. The order was successfully retrieved and validated.");

        logger.info("Delete an order with ID: {}", orderId);

        Response deleteResponse = storeApiClient.deleteOrder(orderId);
        deleteResponse.then()
                .statusCode(HttpStatus.SC_OK)
                .body("code", equalTo(200))
                .body("message", equalTo(String.valueOf(orderId)));

        System.out.println("Test finished. The order was successfully deleted.");
    }

    @Test(description = "Verify that a request for a non-existent order returns a 404 Not Found", groups = {"REGRESSION"})
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
    public void getInventoryByStatusTest() {
        logger.info("Step 1: Get initial inventory count for 'available' pets.");
        Response initialResponse = storeApiClient.getInventoryByStatus();
        String petStatus = "available2025";
        int initialAvailableCount = initialResponse.jsonPath().get(petStatus) != null ? initialResponse.jsonPath().getInt(petStatus) : 0;

        logger.info("Step 2: Create a new pet with 'available' status.");
        createNewPet(petStatus);

        logger.info("Step 3: Get updated inventory count.");
        storeApiClient.getInventoryAndVerifyStatusCount(petStatus, initialAvailableCount + 1);

        System.out.println("Test finished. The inventory count for '" + petStatus + "' pets was successfully validated.");
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
