package edu.cqie.cqdemo;

import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.PersonerlMapper;
import edu.cqie.cqdemo.service.PersonerlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CqDemoApplicationTests {
    @Autowired
    private PersonerlService personerlService;

    @Test
    void contextLoads() {
        Users users=personerlService.getUser();
        System.out.println(users);
    }

}
