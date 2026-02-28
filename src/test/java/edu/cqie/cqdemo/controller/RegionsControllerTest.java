package edu.cqie.cqdemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class RegionsControllerTest {
    @Autowired
    private RegionsController regionsController;
    @Test
    void getRegionsIdAndName() {
        regionsController.getRegionsIdAndName();
    }

    @Test
    void popularityComparison() {
        regionsController.popularityComparison();
    }
}