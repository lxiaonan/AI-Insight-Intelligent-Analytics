package com.xiaonan.xnbi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import generator.domain.Chart;
import com.xiaonan.xnbi.service.ChartService;
import com.xiaonan.xnbi.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author 罗宇楠
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2023-11-29 22:22:32
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




