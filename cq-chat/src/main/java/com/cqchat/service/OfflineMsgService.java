package com.cqchat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqchat.entity.ChatMsg;
import com.cqchat.entity.OfflineMsg;
import com.cqchat.mapper.ChatMsgMapper;
import com.cqchat.mapper.OfflineMsgMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfflineMsgService {

    @Autowired
    private OfflineMsgMapper offlineMsgMapper;

    @Autowired
    private ChatMsgMapper chatMsgMapper;

    public void saveOfflineMsg(Long userId, String msgId) {
        OfflineMsg offlineMsg = new OfflineMsg();
        offlineMsg.setUserId(userId);
        offlineMsg.setMsgId(msgId);
        offlineMsg.setIsDelivered(0);
        offlineMsgMapper.insert(offlineMsg);
    }

    public List<ChatMsg> getUndeliveredMessages(Long userId) {
        // 查询未送达的离线消息
        List<OfflineMsg> offlineMsgs = offlineMsgMapper.selectList(
                new LambdaQueryWrapper<OfflineMsg>()
                        .eq(OfflineMsg::getUserId, userId)
                        .eq(OfflineMsg::getIsDelivered, 0)
                        .orderByAsc(OfflineMsg::getCreatedAt)
        );

        if (offlineMsgs.isEmpty()) {
            return List.of();
        }

        // 提取 msgId 列表
        List<String> msgIds = offlineMsgs.stream()
                .map(OfflineMsg::getMsgId)
                .collect(Collectors.toList());

        // 查询完整消息
        List<ChatMsg> messages = chatMsgMapper.selectList(
                new LambdaQueryWrapper<ChatMsg>()
                        .in(ChatMsg::getMsgId, msgIds)
                        .orderByAsc(ChatMsg::getCreatedAt)
        );

        // 标记为已送达
        offlineMsgMapper.markAllDelivered(userId);

        return messages;
    }

    public int getUndeliveredCount(Long userId) {
        return offlineMsgMapper.selectCount(
                new LambdaQueryWrapper<OfflineMsg>()
                        .eq(OfflineMsg::getUserId, userId)
                        .eq(OfflineMsg::getIsDelivered, 0)
        ).intValue();
    }
}
