package com.myapi.tests.tests;

import com.myapi.tests.specs.ApiSpecifications;
import org.testng.annotations.BeforeMethod;

public class BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void setupSpecifications() {
        System.out.println("Initializing API specifications...");
        ApiSpecifications.initSpecs();
    }


}
