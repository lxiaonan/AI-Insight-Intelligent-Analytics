package com.xiaonan.xnbi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.xiaonan.xnbi.mapper.ImageMapper;
import com.xiaonan.xnbi.model.entity.Image;
import com.xiaonan.xnbi.service.ImageService;
import org.springframework.stereotype.Service;

/**
* @author 罗宇楠
* @description 针对表【image(图片分析表)】的数据库操作Service实现
* @createDate 2023-12-13 22:42:19
*/
@Service
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image>
    implements ImageService {

}




