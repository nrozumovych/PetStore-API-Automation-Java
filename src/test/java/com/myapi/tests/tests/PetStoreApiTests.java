package com.myapi.tests.tests;

import com.myapi.tests.api.PetApiClient;
import com.myapi.tests.models.Category;
import com.myapi.tests.models.Pet;
import com.myapi.tests.models.Tag;
import com.myapi.tests.specs.ApiSpecifications;
import io.qameta.allure.Description;
import io.qameta.allure.Flaky;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.hamcrest.Matchers.*;

public class PetStoreApiTests extends BaseTest {
    protected PetApiClient petApiClient;
    protected Integer createdPetId;

    @BeforeMethod(alwaysRun = true)
    public void setupPetClient() {
        System.out.println("Initializing PetApiClient...");
        this.petApiClient = new PetApiClient();
    }

    private Pet createRandomPet() {
        Integer uniquePetId = new Random().nextInt(99_000_000) + 1_000_000;

        List<Tag> initialTags = new ArrayList<>(Arrays.asList(
                Tag.builder().id(10).name("Friendly").build(),
                Tag.builder().id(11).name("Cute").build()
        ));

        Pet pet = Pet.builder()
                .id(uniquePetId)
                .name("TestPet_" + System.currentTimeMillis())
                .category(Category.builder().id(1).name("Dogs").build())
                .photoUrls(Arrays.asList("http://example.com/photo1.jpg", "http://example.com/photo2.jpg"))
                .tags(initialTags)
                .status("available")
                .build();

        Response createResponse = petApiClient.addPet(pet);
        createResponse.then().spec(ApiSpecifications.responseSpec200);

        Integer apiActualId = createResponse.jsonPath().getInt("id");
        if (!apiActualId.equals(uniquePetId)) {
            System.out.println("Warning: API changed pet ID from " + uniquePetId + " to " + apiActualId);
            pet.setId(apiActualId);
        }

        this.createdPetId = pet.getId();
        return pet;
    }

    @AfterMethod
    public void cleanupPet() {
        if (createdPetId != null) {
            System.out.println("Cleaning up pet with ID: " + createdPetId);
            try {
                petApiClient.deletePetRawResponse(createdPetId)
                        .then()
                        .statusCode(anyOf(equalTo(200), equalTo(404)));
            } catch (Exception e) {
                System.err.println("Failed to delete pet " + createdPetId + ": " + e.getMessage());
            } finally {
                createdPetId = null;
            }
        }
    }

    @Test(description = "Verify that the service is running and returns available pets", groups = {"SMOKE"})
    @Description("This test performs a simple GET request to the /pet/findByStatus endpoint to ensure the API service is active and responding with a 200 OK status code for the 'available' status.")
    public void checkServiceHealth() {
        petApiClient.getPetsByStatus("available");
        System.out.println("Service Health Check Completed.");
    }

    @Test(description = "Create a new pet and then retrieve it by ID to verify its creation", groups = {"SMOKE"})
    @Description("This test creates a new pet with random data and then verifies that the pet's details (ID, name, category, status, etc.) are correctly stored and can be retrieved via the API.")
    public void createAndGetPetTest() {
        Pet newPet = createRandomPet();

        petApiClient.getPetById(newPet.getId()).then()
                .body("id", equalTo(newPet.getId()))
                .body("name", equalTo(newPet.getName()))
                .body("category.name", equalTo(newPet.getCategory().getName()))
                .body("status", equalTo("available"))
                .body("photoUrls", hasSize(newPet.getPhotoUrls().size()))
                .body("tags.id", containsInAnyOrder(
                        newPet.getTags().stream().map(Tag::getId).toArray(Integer[]::new)
                ))
                .body("tags.name", containsInAnyOrder(
                        newPet.getTags().stream().map(Tag::getName).toArray(String[]::new)
                ));

        System.out.println("Create and Get Pet Test Passed for ID: " + newPet.getId());
    }

    @Test(description = "Update an existing pet's details and verify the changes", groups = {"SMOKE"})
    @Description("This test creates a new pet, updates its name and status, and then verifies that the changes are correctly reflected in the API response and when retrieving the pet again by ID.")
    @Flaky
    public void updatePetTest() {
        Pet petToUpdate = createRandomPet();

        String updatedName = "UpdatedBuddy_" + System.currentTimeMillis();
        String updatedStatus = "sold";
        petToUpdate.setName(updatedName);
        petToUpdate.setStatus(updatedStatus);
        petToUpdate.getTags().add(Tag.builder().id(12).name("Loyal").build());

        Response updateResponse = petApiClient.updatePet(petToUpdate);

        updateResponse.then()
                .body("id", equalTo(petToUpdate.getId()))
                .body("name", equalTo(petToUpdate.getName()))
                .body("status", equalTo(petToUpdate.getStatus()))
                .body("tags.size()", equalTo(petToUpdate.getTags().size()))
                .body("tags.name", hasItem("Loyal"));

        System.out.println("Updated Pet: " + updateResponse.asString());

        petApiClient.getPetById(petToUpdate.getId()).then()
                .body("name", equalTo(updatedName))
                .body("status", equalTo(updatedStatus))
                .body("tags.name", hasItem("Loyal"));

        System.out.println("Update Pet Test Passed for ID: " + petToUpdate.getId());
    }

    @Test(description = "Delete a pet and verify it can no longer be retrieved", groups = {"SMOKE"})
    @Description("This test performs a full delete lifecycle: it creates a pet, deletes it via the API, and then verifies that a subsequent GET request returns a 404 Not Found status code.")
    public void deletePetTest() {
        Pet petToDelete = createRandomPet();

        petApiClient.getPetById(petToDelete.getId());

        Response deleteResponse = petApiClient.deletePet(petToDelete.getId());

        deleteResponse.then()
                .body("code", equalTo(200))
                .body("message", equalTo(String.valueOf(petToDelete.getId())));

        System.out.println("Deleted Pet: " + deleteResponse.asString());

        petApiClient.getPetByIdExpectingError(petToDelete.getId())
                .then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("message", equalTo("Pet not found"));

        System.out.println("Delete Pet Test Passed for ID: " + petToDelete.getId());
    }

    @Test(description = "Verify that API returns 404 when trying to get a non-existent pet", groups = {"REGRESSION"})
    @Description("This test confirms the API's behavior for an invalid request. It attempts to retrieve a pet with a non-existent ID and validates that the service responds with the correct HTTP 404 Not Found status code and a 'Pet not found' message.")
    public void getNonExistentPetTest() {
        Integer nonExistentPetId = -1;

        Response response = petApiClient.getPetByIdExpectingError(nonExistentPetId);

        response.then()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .body("message", equalTo("Pet not found"));

        System.out.println("Get Non-Existent Pet Test Passed.");
    }
}