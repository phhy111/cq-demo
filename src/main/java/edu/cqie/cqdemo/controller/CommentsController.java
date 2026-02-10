package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.ScenicsCommentsDTO;
import edu.cqie.cqdemo.entity.Comments;
import edu.cqie.cqdemo.service.CommentsService;
import edu.cqie.cqdemo.util.JwtUtil;
import edu.cqie.cqdemo.util.OSSOperationUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/comments")
public class CommentsController {
    @Autowired
    private CommentsService commentsService;

    @Autowired
    private JwtUtil jwtUtil;  // 注入 JWT 工具类

    @Autowired
    private OSSOperationUtil ossOperationUtil;
    /**
     * 获取景点评论信息
     * @param id 景点id
     * @return 景点评论信息
     */
    @GetMapping("/GetScenicsCommentsInfo")
    public Result<List<ScenicsCommentsDTO>> getScenicsCommentsInfo(Integer id) {
        List<ScenicsCommentsDTO> commentsInfo = commentsService.getScenicsCommentsInfo(id);
        if (commentsInfo != null){
            return Result.success(commentsInfo);
        }else {
            return Result.error("未查询到该景点的评论信息");
        }
    }

    /**
     * 添加评论信息
     * @param comments 评论信息
     * @return 添加结果
     */
    @PostMapping("/AddCommentsInfo")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> addCommentsInfo(
            @RequestPart(required = false) MultipartFile file,
            @RequestPart Comments comments, // 前端传的comments中无userId
            HttpServletRequest request) {
        try {
            // 1. Token解析（原有逻辑）
            String token = jwtUtil.getTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("无效的Token");
            }
            Long userId = jwtUtil.getUserIdFromToken(token);

            // 2. 关键修复：将解析到的userId设置到comments实体中
            comments.setUserId(userId); // 之前漏掉了这一步！

            // 3. 填充时间（原有逻辑）
            comments.setCreatedAt(new Date());
            comments.setUpdatedAt(new Date());

            // 4. 图片上传（原有逻辑）
            if (file != null && !file.isEmpty()) {
                String imageUrl = ossOperationUtil.upload(file);
                comments.setImages(imageUrl);
            } else {
                comments.setImages("");
            }

            // 5. 保存评论
            boolean success = commentsService.addCommentWithUserInfo(comments);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("评论添加失败");
            }
        } catch (Exception e) {
            log.error("添加评论失败", e);
            return Result.error("服务器内部错误：" + e.getMessage());
        }
    }
}
