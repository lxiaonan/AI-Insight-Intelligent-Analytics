package com.xiaonan.xnbi.utils;

import com.xiaonan.xnbi.model.dto.chart.AIResultDto;

public class AiUtils {
    public static AIResultDto getAns(String question) {
        BigModelNew bigModelNew = new BigModelNew();
        bigModelNew.getResult(question);
        String aReturn = bigModelNew.getReturn();
        String chartData = "服务错误";
        String onAnalysis = "服务错误";
        if(aReturn.contains("：") && aReturn.contains("然后输出【【【【【"))
            onAnalysis = aReturn.substring(aReturn.indexOf("：") + 1,aReturn.indexOf("然后输出【【【【【"));
        if(aReturn.contains("```javascript")){
            aReturn = aReturn.substring(aReturn.indexOf("```javascript"));
        }
        if(aReturn.contains("option") && aReturn.contains("};")){
            chartData = aReturn.substring(aReturn.indexOf("option"),aReturn.indexOf("};") + 2);
        }

        AIResultDto aiResultDto = new AIResultDto();
        aiResultDto.setChartData(chartData.trim());
        aiResultDto.setOnAnalysis(onAnalysis.trim());

        return aiResultDto;
    }
}
