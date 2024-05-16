package com.xiaonan.xnbi.utils.image;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;

import java.io.File;

public class AiImageUtils {
    private final RedissonClient redissonClient;
    public AiImageUtils(RedissonClient redissonClient){
        this.redissonClient = redissonClient;
    }
    public String getAns(File file, String question,Long id) {
        BigModelImage bigModelImage = new BigModelImage(file,redissonClient,id);
        bigModelImage.getResult(question);
        String res = bigModelImage.getReturn();
        if(StringUtils.isBlank(res)){
            throw new RuntimeException("AI分析图片异常");
        }
        return res;
    }
}
