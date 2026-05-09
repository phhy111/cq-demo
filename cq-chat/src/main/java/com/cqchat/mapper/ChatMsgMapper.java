package com.cqchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqchat.entity.ChatMsg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatMsgMapper extends BaseMapper<ChatMsg> {

    @Update("UPDATE chat_msg SET is_read = 1 WHERE conversation_id = #{conversationId} AND id <= #{msgId} AND is_read = 0")
    int markAsRead(@Param("conversationId") Long conversationId, @Param("msgId") Long msgId);
}
