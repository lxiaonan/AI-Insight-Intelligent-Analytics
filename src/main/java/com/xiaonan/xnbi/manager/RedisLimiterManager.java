package com.xiaonan.xnbi.manager;

import com.xiaonan.xnbi.common.ErrorCode;
import com.xiaonan.xnbi.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RedisLimiterManager {
    @Resource
    RedissonClient redissonClient;

    /***
     * 限流操作
     * @param key 区分不同的限流器，比如不同的用户 id 应该分别统计
     * */
    public void doRateLimit(String key) {
        // 创建一个名称为user_limiter的限流器，每12秒最多访问 1 次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, 1, 30, RateIntervalUnit.SECONDS);
        // 每当一个操作来了后，请求一个令牌
        boolean canOp = rateLimiter.tryAcquire(1);
        if (!canOp) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }

    }
}
