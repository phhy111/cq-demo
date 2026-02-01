package edu.cqie.cqdemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ScenicsControllerTest {
    @Autowired
    private ScenicsController scenicsController;

    @Test
    void getScenicsInfo() {
        scenicsController.getScenicsInfo();
    }

    @Test
    void getSlideShowInfo() {
        scenicsController.getSlideShowInfo();
    }

    @Test
    void getScenicsInfoByRegionId() {
        scenicsController.getScenicsInfoByRegionId(1);
    }
}