package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.ScenicsCommentsDTO;
import edu.cqie.cqdemo.entity.Comments;
import edu.cqie.cqdemo.service.CommentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentsController {
    @Autowired
    private CommentsService commentsService;

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
    public Result<Boolean> addCommentsInfo(Comments comments) {
        boolean result = commentsService.insertCommentsInfo(comments);
        if (result){
            return Result.success(result);
        }else {
            return Result.error("添加评论信息失败");
        }
    }
}
