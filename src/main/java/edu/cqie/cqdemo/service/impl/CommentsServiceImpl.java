package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.dto.ScenicsCommentsDTO;
import edu.cqie.cqdemo.entity.Comments;
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.UserMapper;
import edu.cqie.cqdemo.service.CommentsService;
import edu.cqie.cqdemo.mapper.CommentsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


}




