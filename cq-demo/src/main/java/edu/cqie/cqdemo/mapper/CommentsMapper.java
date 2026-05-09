package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.dto.ScenicsCommentsDTO;
import edu.cqie.cqdemo.dto.CommentsDTO;
import edu.cqie.cqdemo.entity.Comments;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author
* @description 针对表【comments(评论表)】的数据库操作Mapper
* @createDate 2026-02-03 22:06:59
* @Entity edu.cqie.cqdemo.entity.Comments
*/
@Mapper
public interface CommentsMapper extends BaseMapper<Comments> {
    /**
     * 获取景区评论信息
     * @param id
     */
    List<ScenicsCommentsDTO> getScenicsCommentsInfo(Integer id);
    /**
     * 插入评论信息
     * @param
     */
    boolean insertCommentsInfo(Comments  comment);

    //获取相应评论
    List<CommentsDTO> getCommentDetail(Integer targetId,Integer targetType);
    
    /**
     * 获取评论的回复列表
     * @param commentId 评论ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 回复列表
     */
    List<CommentsDTO> getCommentReplies(Integer commentId, Integer page, Integer size);
    
    /**
     * 获取直接回复某个评论的所有子评论
     * @param commentId 评论ID
     * @return 直接回复列表
     */
    List<CommentsDTO> getDirectReplies(Integer commentId);
}




