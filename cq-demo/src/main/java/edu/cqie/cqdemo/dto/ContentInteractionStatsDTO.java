package edu.cqie.cqdemo.dto;

import lombok.Data;

@Data
public class ContentInteractionStatsDTO {
    /**
     * 近1个月发布的内容总数
     */
    private Integer publishContentTotal;

    /**
     * 近1个月的互动总数（点赞+评论+收藏）
     */
    private Integer interactionTotal;
}
