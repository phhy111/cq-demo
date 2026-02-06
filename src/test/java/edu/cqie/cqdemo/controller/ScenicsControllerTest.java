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

    @Test
    void getScenicsInfoByLevel() {
        scenicsController.getScenicsInfoByLevel("null");
    }

    @Test
    void getScenicsInfoByScore() {
        scenicsController.getScenicsInfoByScore();
    }

    @Test
    void getScenicsInfoByLike() {
        scenicsController.getScenicsInfoByLike();
    }

    @Test
    void addViewCount() {
        scenicsController.addViewCount(1);
    }

    @Test
    void getScenicsDetailInfoById() {
        scenicsController.getScenicsDetailInfoById(1);
    }
}