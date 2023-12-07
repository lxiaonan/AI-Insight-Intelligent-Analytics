package com.xiaonan.xnbi.mq;

import com.rabbitmq.client.Channel;
import com.xiaonan.xnbi.common.ErrorCode;
import com.xiaonan.xnbi.exception.BusinessException;
import com.xiaonan.xnbi.manager.RedisLimiterManager;
import com.xiaonan.xnbi.model.dto.chart.AIResultDto;
import com.xiaonan.xnbi.model.entity.Chart;
import com.xiaonan.xnbi.model.enums.ChartStateEnum;
import com.xiaonan.xnbi.service.ChartService;
import com.xiaonan.xnbi.utils.AiUtils;
import com.xiaonan.xnbi.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class BiConsumer {
    @Resource
    ChartService chartService;
    @Resource
    RedisLimiterManager redisLimiterManager;
    @Resource
    RedissonClient redissonClient;


    /**
     * 监听消息处理
     * @param message
     * @param channel
     * @param deliveryTag
     */
    @RabbitListener(queues = {"bi_queue"},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        if(StringUtils.isBlank(message)){
            log.error("信息为空");
            //空消息是没有价值的，直接确认
            try {
                channel.basicAck(deliveryTag,false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        //解析消息
        String msg[] = message.split(",");
        //从数据库查询当前这个图表
        Chart chart = chartService.getById(Long.valueOf(msg[0]));
        String goal = chart.getGoal();
        String data = chart.getChartData();
        String chartType = chart.getChartType();
        //生成ai提问的消息
        StringBuffer res = new StringBuffer();
        res.append("你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：");
        res.append("\n").append("分析需求：").append("\n").append("{").append(goal).append("}").append("\n");

        res.append("原始数据:").append("\n").append(data);
        res.append("请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n【【【【【\n先输出上面原始数据的分析结果：\n然后输出【【【【【\n{前端 Echarts V5 的 option 配置对象JSON代码JSON代码JSON代码，生成");
        res.append(chartType);
        res.append("合理地将数据进行可视化，不要生成任何多余的内容，不要注释}");

        chart.setState(ChartStateEnum.RUNNING.getValue());
        boolean update = chartService.updateById(chart);
        if (!update) {
            try {
                channel.basicNack(deliveryTag,false,true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            chartService.handleChartUpdateError(chart.getId(), "更新图表失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图表失败");
        }
        //限流
        redisLimiterManager.doRateLimit("aiLimiter:" + msg[1]);
        AiUtils aiUtils = new AiUtils(redissonClient);
        AIResultDto ans = aiUtils.getAns(chart.getId(), res.toString());
        String chartData = ans.getChartData();
        String onAnalysis = ans.getOnAnalysis();
        if (!chartData.equals("服务错误") && !onAnalysis.equals("服务错误")) {
            Chart succeedChart = new Chart();
            succeedChart.setId(chart.getId());
            succeedChart.setState(ChartStateEnum.SUCCEED.getValue());
            succeedChart.setGenChart(chartData);
            succeedChart.setGenResult(onAnalysis);
            boolean success = chartService.updateById(succeedChart);
            if (!success) {
                try {
                    channel.basicNack(deliveryTag,false,false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                chartService.handleChartUpdateError(chart.getId(), "更新图表失败");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图表失败");
            }
        } else {
            //确定为失败消息，消息重复执行
            try {
                channel.basicNack(deliveryTag,false,true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            chartService.handleChartUpdateError(chart.getId(), "Ai生成图表失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Ai生成图表失败");
        }
        //生成完成
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.debug("确定消息失败");
            throw new RuntimeException(e);
        }
    }
}
