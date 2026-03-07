// src/test/java/edu/cqie/cqdemo/AiServiceTest.java
package edu.cqie.cqdemo;


import edu.cqie.cqdemo.mapper.FoodCategoriesMapper;
import edu.cqie.cqdemo.service.AiService;
import edu.cqie.cqdemo.service.FoodCategoriesService;
import edu.cqie.cqdemo.service.ShopService;
import edu.cqie.cqdemo.service.impl.FoodCategoriesServiceImpl;
import edu.cqie.cqdemo.service.impl.ShopServiceImpI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CqDemoApplicationTests {

    @Autowired
    private FoodCategoriesServiceImpl foodCategoriesServiceImpl;
    @Autowired
    private FoodCategoriesService foodCategoriesService;


    @Test

    void contextLoads() {
        foodCategoriesService.selectallFoods();
}}