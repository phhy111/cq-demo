package edu.cqie.cqdemo.controller;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FoodCategoriesControllerTest {
    @Autowired
    private FoodCategoriesController foodCategoriesController;

    @Test
    void getRecommendedFoods() {
        foodCategoriesController.getRecommendedFoods();
    }
}