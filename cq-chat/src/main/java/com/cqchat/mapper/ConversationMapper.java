package com.cqchat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cqchat.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversation WHERE " +
            "(user_a_id = #{userA} AND user_b_id = #{userB}) OR " +
            "(user_a_id = #{userB} AND user_b_id = #{userA}) LIMIT 1")
    Conversation findByTwoUsers(@Param("userA") Long userA, @Param("userB") Long userB);
}
