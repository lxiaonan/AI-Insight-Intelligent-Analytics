package com.xiaonan.xnbi.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class RedisLimiterManagerTest {
    @Resource
    RedisLimiterManager redisLimiterManage;
    @Test
    void doRateLimit() throws InterruptedException {
        String key = "123";
        redisLimiterManage.doRateLimit(key);
        System.out.println("成功");
        Thread.sleep(13);
        redisLimiterManage.doRateLimit(key);
    }
}
