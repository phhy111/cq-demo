package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.BrowsingHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BrowsingHistoryMapper extends BaseMapper<BrowsingHistory> {
    /**
     * 根据用户ID和类型获取浏览历史
     */
    List<BrowsingHistory> selectByUserIdAndType(@Param("userId") Long userId, @Param("type") Integer type);

    /**
     * 根据用户ID获取所有浏览历史
     */
    List<BrowsingHistory> selectByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的浏览历史
     */
    int deleteByUserIdAndBusinessId(@Param("userId") Long userId, @Param("businessId") Long businessId, @Param("type") Integer type);

    /**
     * 删除用户的所有浏览历史
     */
    int deleteByUserId(@Param("userId") Long userId);
}
