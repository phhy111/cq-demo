package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.entity.Scenics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

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

    @Test
    void selectScenicsAndRegionPage() {
        scenicsController.selectScenicsAndRegionPage(2,5);
    }

    @Test
    void deleteScenics() {
        scenicsController.deleteScenics(List.of(17));
    }

    @Test
    void getRecommendedScenics() {
        scenicsController.getRecommendedScenics();
    }

    @Test
    void getRegionScenicsCount() {
        scenicsController.getRegionScenicsCount();
    }
}