package com.xiaonan.xnbi.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChartByAIRequest implements Serializable {
    /**
     * 分析目标
     */
    private String goal;
    /**
     * 图表名称
     */
    private String name;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}
