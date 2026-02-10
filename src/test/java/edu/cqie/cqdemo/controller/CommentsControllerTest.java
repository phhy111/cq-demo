package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Comments;
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest
class CommentsControllerTest {

    @Autowired
    private CommentsController commentsController;
    @Mock
    private JwtUtil jwtUtil;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // 初始化 Mockito 注解
    }
    @Test
    void getScenicsCommentsInfo() {
        commentsController.getScenicsCommentsInfo(1);
    }

    @Test
    void addCommentsInfo() {

    }


//    @Test
//    void addCommentsInfo() {
//        Comments comments = new Comments();
//        comments.setUserId(2L);
//        comments.setTargetId(1);
//        comments.setTargetType(1);
//        comments.setParentId(0);
//        comments.setReplyToId(null);
//        comments.setContent("测试评论");
//        comments.setImages("");
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        request.addHeader("Authorization", "Bearer validToken");
//        when(jwtUtil.getTokenFromRequest(request)).thenReturn("validToken");
//        when(jwtUtil.validateToken("validToken")).thenReturn(true);
//        when(jwtUtil.getUserIdFromToken("validToken")).thenReturn(2L);
//        Result<Boolean> result = commentsController.addCommentsInfo(comments, request);
//        assertEquals(Result.success(true), result);
//    }
}