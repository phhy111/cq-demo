package edu.cqie.cqdemo.dto;

import lombok.Data;

@Data
public class RegionScenicsCountDTO {
    /**
     * 区域ID
     */
    private Integer regionId;

    /**
     * 区域名称
     */
    private String name;

    /**
     * 景点数量
     */
    private Long scnicsCount;
}
