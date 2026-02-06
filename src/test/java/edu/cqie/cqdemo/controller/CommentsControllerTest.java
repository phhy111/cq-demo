package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.entity.Comments;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CommentsControllerTest {

    @Autowired
    private CommentsController commentsController;

    @Test
    void getScenicsCommentsInfo() {
        commentsController.getScenicsCommentsInfo(1);
    }
    @Test
    void addCommentsInfo() {
        Comments comments = new Comments();
        comments.setUserId(1L);
        comments.setTargetId(2);
        comments.setTargetType(1);
        comments.setParentId(0);
        comments.setContent("解放碑挺好玩的");
        commentsController.addCommentsInfo(comments);
    }
}