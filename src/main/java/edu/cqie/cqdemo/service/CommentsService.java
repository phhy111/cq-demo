package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.dto.ScenicsCommentsDTO;
import edu.cqie.cqdemo.dto.CommentsDTO;
import edu.cqie.cqdemo.entity.Comments;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author
* @description 针对表【comments(评论表)】的数据库操作Service
* @createDate 2026-02-03 22:06:59
*/
public interface CommentsService extends IService<Comments> {
    /**
     * 获取景点评论信息
     * @param id
     * @return
     */
    List<ScenicsCommentsDTO> getScenicsCommentsInfo(Integer id);

    /**
     * 添加评论
     * @param comments
     * @return
     */
// 添加评论的业务方法（带用户信息）
    boolean addCommentWithUserInfo(Comments comments);

    List<CommentsDTO> getRoutesComments(Integer targetId,Integer targetType);
    
    /**
     * 获取评论的回复列表
     * @param commentId 评论ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 回复列表
     */
    List<CommentsDTO> getCommentReplies(Integer commentId, Integer page, Integer size);
    
    /**
     * 递归获取评论的所有回复（包括子评论的子评论）
     * @param commentId 评论ID
     * @return 回复树结构
     */
    List<CommentsDTO> getCommentRepliesRecursive(Integer commentId);
}
