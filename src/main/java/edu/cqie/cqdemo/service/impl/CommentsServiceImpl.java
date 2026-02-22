package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.dto.ScenicsCommentsDTO;
import edu.cqie.cqdemo.dto.CommentsDTO;
import edu.cqie.cqdemo.entity.Comments;
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.UserMapper;
import edu.cqie.cqdemo.service.CommentsService;
import edu.cqie.cqdemo.mapper.CommentsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.xml.stream.events.Comment;
import java.util.List;

/**
 * @author
 * @description 针对表【comments(评论表)】的数据库操作Service实现
 * @createDate 2026-02-03 22:06:59
 */
@Service
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments>
        implements CommentsService{
    @Autowired
    private CommentsMapper commentsMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    Users user = new Users();

    @Override
    public List<ScenicsCommentsDTO> getScenicsCommentsInfo(Integer id) {
        return commentsMapper.getScenicsCommentsInfo(id);
    }

    //添加用户评论
    @Override
    public boolean addCommentWithUserInfo(Comments comments)
    {
        return commentsMapper.insertCommentsInfo(comments);
    }


    @Override
    public List<CommentsDTO> getRoutesComments(Integer targetId, Integer targetType)
    {
        List<CommentsDTO> comments = commentsMapper.getCommentDetail(targetId, targetType);
        // 更新点赞数从Redis
        updateCommentLikeCountsFromRedis(comments);
        return comments;
    }

    /**
     * 从Redis更新评论的点赞数
     */
    private void updateCommentLikeCountsFromRedis(List<CommentsDTO> comments) {
        if (comments != null && !comments.isEmpty()) {
            try {
                for (CommentsDTO comment : comments) {
                    // 从Redis获取点赞数
                    String redisKey = "likes:5:" + comment.getId();
                    Long likeCount = redisTemplate.opsForSet().size(redisKey);
                    if (likeCount != null) {
                        comment.setLikeCount(likeCount.intValue());
                    }
                    // 递归更新子评论
                    if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
                        updateCommentLikeCountsFromRedis(comment.getReplies());
                    }
                }
            } catch (Exception e) {
                // Redis读取失败，使用数据库值
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<CommentsDTO> getCommentReplies(Integer commentId, Integer page, Integer size) {
        List<CommentsDTO> replies = commentsMapper.getCommentReplies(commentId, page, size);
        // 更新点赞数从Redis
        updateCommentLikeCountsFromRedis(replies);
        return replies;
    }

    @Override
    public List<CommentsDTO> getCommentRepliesRecursive(Integer commentId) {
        // 获取直接回复
        List<CommentsDTO> directReplies = commentsMapper.getDirectReplies(commentId);
        // 递归获取每个直接回复的子回复
        for (CommentsDTO reply : directReplies) {
            List<CommentsDTO> childReplies = getCommentRepliesRecursive(reply.getId());
            reply.setReplies(childReplies);
        }
        // 更新点赞数从Redis
        updateCommentLikeCountsFromRedis(directReplies);
        return directReplies;
    }

    @Override
    public void updateCommentLikeCount(Integer commentId, int delta) {
        // 获取评论
        Comments comment = getById(commentId);
        if (comment != null) {
            // 更新点赞数，确保不小于0
            int newLikeCount = Math.max(0, comment.getLikeCount() + delta);
            comment.setLikeCount(newLikeCount);
            // 保存更新
            updateById(comment);
        }
    }
}




