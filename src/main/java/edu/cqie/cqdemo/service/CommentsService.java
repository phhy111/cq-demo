package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.dto.ScenicsCommentsDTO;
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
}
