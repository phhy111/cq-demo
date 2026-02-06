package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.dto.ScenicsCommentsDTO;
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
    public List<ScenicsCommentsDTO> getScenicsCommentsInfo(Integer id);
    /**
     * 插入评论信息
     * @param comments
     */
    public boolean insertCommentsInfo(Comments comments);
}




