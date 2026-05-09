package edu.cqie.cqdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.cqie.cqdemo.entity.Guides;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GuidesMapper extends BaseMapper<Guides> {
    /**
     * 更新攻略的点赞数、收藏数和评论数
     */
    void updateLikeCountAndCollectCount();
}
