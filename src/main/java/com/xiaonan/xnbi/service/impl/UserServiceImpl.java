package com.xiaonan.xnbi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaonan.xnbi.model.entity.User;
import com.xiaonan.xnbi.service.UserService;
import com.xiaonan.xnbi.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author 罗宇楠
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2023-11-29 22:22:32
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




