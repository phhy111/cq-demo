package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.entity.Users;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserControllerTest {
    @Autowired
    private UserController userController;

    @Test
    void getUserInfo() {
        userController.getUserInfo();
    }

    @Test
    void toggleStatus() {
        userController.toggleStatus(1L);
    }
    Users users = new Users();
    @Test
    void resetPassword() {

    }

    @Test
    void batchDisable() {
        List<Long> userIds = Arrays.asList(1L, 3L, 4L);
        userController.batchDisable(userIds);
    }
}