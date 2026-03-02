// src/test/java/edu/cqie/cqdemo/AiServiceTest.java
package edu.cqie.cqdemo;


import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Food;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.mapper.CollectMapper;
import edu.cqie.cqdemo.mapper.FoodCategoriesMapper;
import edu.cqie.cqdemo.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CqDemoApplicationTests {

    @Autowired
    private AiService aiService;
    @Autowired
    private CollectMapper collectMapper;
    @Autowired
    private FoodCategoriesMapper foodCategoriesMapper;
    @Autowired
    private
    @Test

    void contextLoads() {


        System.out.println(foodCategoriesMapper.selectFoods());

}}