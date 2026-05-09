package com.cqchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqchat.entity.OfflineMsg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OfflineMsgMapper extends BaseMapper<OfflineMsg> {

    @Update("UPDATE offline_msg SET is_delivered = 1 WHERE user_id = #{userId} AND is_delivered = 0")
    int markAllDelivered(@Param("userId") Long userId);
}
