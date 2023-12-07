package com.xiaonan.xnbi.controller;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaonan.xnbi.annotation.AuthCheck;
import com.xiaonan.xnbi.common.BaseResponse;
import com.xiaonan.xnbi.common.DeleteRequest;
import com.xiaonan.xnbi.common.ErrorCode;
import com.xiaonan.xnbi.common.ResultUtils;
import com.xiaonan.xnbi.constant.UserConstant;
import com.xiaonan.xnbi.exception.BusinessException;
import com.xiaonan.xnbi.exception.ThrowUtils;
import com.xiaonan.xnbi.manager.RedisLimiterManager;
import com.xiaonan.xnbi.model.dto.chart.*;
import com.xiaonan.xnbi.model.entity.Chart;
import com.xiaonan.xnbi.model.entity.User;
import com.xiaonan.xnbi.model.enums.ChartStateEnum;
import com.xiaonan.xnbi.mq.MyMessageProducer;
import com.xiaonan.xnbi.service.ChartService;
import com.xiaonan.xnbi.service.UserService;
import com.xiaonan.xnbi.utils.AiUtils;
import com.xiaonan.xnbi.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 帖子接口
 *
 * @author <a href="https://github.com/lixiaonan">小楠</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;
    @Resource
    private UserService userService;

    @Resource
    RedissonClient redissonClient;

    @Resource
    RedisLimiterManager redisLimiterManager;

    @Resource
    ThreadPoolExecutor threadPoolExecutor;

    @Resource
    MyMessageProducer myMessageProducer;
    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);

        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());

        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        // 参数校验

        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest).orderByDesc("createTime"));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest).orderByDesc("createTime"));
        return ResultUtils.success(chartPage);
    }

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);

        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 同步获取图表
     *
     * @param file
     * @param chartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("gen")
    public BaseResponse<AIResultDto> genChartByAi(@RequestPart("file") MultipartFile file
            , ChartByAIRequest chartByAIRequest, HttpServletRequest request) {
        //获取当前用户id
        User loginUser = userService.getLoginUser(request);
        String goal = chartByAIRequest.getGoal();
        String name = chartByAIRequest.getName();
        String chartType = chartByAIRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() >= 64, ErrorCode.PARAMS_ERROR, "名称过长");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR);
        //判断大小是否超过1MB
        final long ONE_MB = 1024 * 1024L;
        long size = file.getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        //判断文件类型
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(StringUtils.isBlank(suffix), ErrorCode.PARAMS_ERROR, "文件名异常");
        boolean isExcel = suffix.equals("xlsx") || suffix.equals("xls");
        ThrowUtils.throwIf(!isExcel, ErrorCode.PARAMS_ERROR, "文件类型错误");

        //根据用户上传的数据，压缩ai提问语
        StringBuffer res = new StringBuffer();
        res.append("你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：");
        res.append("\n").append("分析需求：").append("\n").append("{").append(goal).append("}").append("\n");

        String data = ExcelUtils.excelToCsv(file);
        res.append("原始数据:").append("\n").append(data);
        res.append("请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n【【【【【\n先输出上面原始数据的分析结果：\n然后输出【【【【【\n{前端 Echarts V5 的 option 配置对象JSON代码，生成");
        res.append(chartType);
        res.append("合理地将数据进行可视化，不要生成任何多余的内容，不要注释}");
        Chart chart = new Chart();
        chart.setName(name);
        chart.setUserId(loginUser.getId());
        chart.setGoal(goal);
        chart.setChartData(data);
        chart.setChartType(chartType);
        //将创建的图表保存到数据库
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "保存失败");
        //限流
        redisLimiterManager.doRateLimit("aiLimiter:" + loginUser.getId());
        AiUtils aiUtils = new AiUtils(redissonClient);
        AIResultDto ans = aiUtils.getAns(chart.getId(), res.toString());

        chart.setGenChart(ans.getChartData());
        chart.setGenResult(ans.getOnAnalysis());
        chart.setState(ChartStateEnum.SUCCEED.getValue());
        save = chartService.updateById(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表状态更新失败");
        ans.setChartId(chart.getId());
        return ResultUtils.success(ans);
    }

    /**
     * 使用rabbitMq，异步获取图表
     *
     * @param file
     * @param chartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("gen_async/mq")
    public BaseResponse<AIResultDto> genChartByAiAsyncMq(@RequestPart("file") MultipartFile file
            , ChartByAIRequest chartByAIRequest, HttpServletRequest request) {
        //获取当前用户id
        User loginUser = userService.getLoginUser(request);
        String goal = chartByAIRequest.getGoal();
        String name = chartByAIRequest.getName();
        String chartType = chartByAIRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() >= 64, ErrorCode.PARAMS_ERROR, "名称过长");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR);
        //判断大小是否超过1MB
        final long ONE_MB = 1024 * 1024L;
        long size = file.getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        //判断文件类型
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(StringUtils.isBlank(suffix), ErrorCode.PARAMS_ERROR, "文件名异常");
        boolean isExcel = suffix.equals("xlsx") || suffix.equals("xls");
        ThrowUtils.throwIf(!isExcel, ErrorCode.PARAMS_ERROR, "文件类型错误");

        //根据用户上传的数据，压缩ai提问语
        String data = ExcelUtils.excelToCsv(file);

        Chart chart = new Chart();
        chart.setName(name);
        chart.setUserId(loginUser.getId());
        chart.setGoal(goal);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setState(ChartStateEnum.WAIT.getValue());
        //将创建的图表保存到数据库
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表创建失败");

        //使用mq发送任务
        try {
            String message = chart.getId() + "," + loginUser.getId();
            myMessageProducer.sendMessage(message);


        } catch (Exception e) {
            chartService.handleChartUpdateError(chart.getId(), "Ai生成图表失败" + e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Ai生成图表失败");
        }

        AIResultDto aiResultDto = new AIResultDto();
        aiResultDto.setChartId(chart.getId());
        return ResultUtils.success(aiResultDto);
    }

    /**
     * 异步获取图表
     *
     * @param file
     * @param chartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("gen_async")
    public BaseResponse<AIResultDto> genChartByAiAsync(@RequestPart("file") MultipartFile file
            , ChartByAIRequest chartByAIRequest, HttpServletRequest request) {
        //获取当前用户id
        User loginUser = userService.getLoginUser(request);
        String goal = chartByAIRequest.getGoal();
        String name = chartByAIRequest.getName();
        String chartType = chartByAIRequest.getChartType();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() >= 64, ErrorCode.PARAMS_ERROR, "名称过长");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType), ErrorCode.PARAMS_ERROR);
        //判断大小是否超过1MB
        final long ONE_MB = 1024 * 1024L;
        long size = file.getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        //判断文件类型
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(StringUtils.isBlank(suffix), ErrorCode.PARAMS_ERROR, "文件名异常");
        boolean isExcel = suffix.equals("xlsx") || suffix.equals("xls");
        ThrowUtils.throwIf(!isExcel, ErrorCode.PARAMS_ERROR, "文件类型错误");

        //根据用户上传的数据，压缩ai提问语
        StringBuffer res = new StringBuffer();
        res.append("你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：");
        res.append("\n").append("分析需求：").append("\n").append("{").append(goal).append("}").append("\n");

        String data = ExcelUtils.excelToCsv(file);
        res.append("原始数据:").append("\n").append(data);
        res.append("请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n【【【【【\n先输出上面原始数据的分析结果：\n然后输出【【【【【\n{前端 Echarts V5 的 option 配置对象JSON代码，生成");
        res.append(chartType);
        res.append("合理地将数据进行可视化，不要生成任何多余的内容，不要注释}");
        Chart chart = new Chart();
        chart.setName(name);
        chart.setUserId(loginUser.getId());
        chart.setGoal(goal);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setState(ChartStateEnum.WAIT.getValue());
        //将创建的图表保存到数据库
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表创建失败");

        //使用线程池执行任务
        try {
            CompletableFuture.runAsync(() -> {
                Chart updateChart = new Chart();
                updateChart.setId(chart.getId());
                updateChart.setState(ChartStateEnum.RUNNING.getValue());
                boolean update = chartService.updateById(updateChart);
                if (!update) {
                    chartService.handleChartUpdateError(chart.getId(), "更新图表失败");
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图表失败");
                }
                //限流
                redisLimiterManager.doRateLimit("aiLimiter:" + loginUser.getId());
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
                        chartService.handleChartUpdateError(chart.getId(), "更新图表失败");
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图表失败");
                    }
                } else {
                    chartService.handleChartUpdateError(chart.getId(), "Ai生成图表失败");
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Ai生成图表失败");
                }
            }, threadPoolExecutor);


        } catch (Exception e) {
            chartService.handleChartUpdateError(chart.getId(), "Ai生成图表失败" + e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Ai生成图表失败");
        }

        AIResultDto aiResultDto = new AIResultDto();
        aiResultDto.setChartId(chart.getId());
        return ResultUtils.success(aiResultDto);
    }

}
