package edu.cqie.cqdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 核心：强制扫描Mapper包，优先级最高
@MapperScan("edu.cqie.cqdemo.mapper")
@SpringBootApplication
public class CqDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CqDemoApplication.class, args);
    }
}