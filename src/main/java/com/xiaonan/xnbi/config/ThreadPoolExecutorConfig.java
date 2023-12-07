package com.xiaonan.xnbi.config;

import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 自定义线程池
 */
@Configuration
@Data
public class ThreadPoolExecutorConfig {
    final int corePoolSize = 1;
    final int maximumPoolSize = 2;
    final long keepAliveTime = 180;
    final TimeUnit unit = TimeUnit.SECONDS;
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        ThreadFactory threadFactory = new ThreadFactory(){
            private int count = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("线程" + count);
                count ++;
                return thread;
            }
        };
        /**
         * 当队列满了后，核心线程数达到后，最大工作线程数还没满的情况下，会开启一个临时的线程来执行操作
         * 这个临时的线程是先取最近先到达的任务
         */
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize,maximumPoolSize,
                keepAliveTime,unit,new ArrayBlockingQueue<>(10),threadFactory);
        return threadPoolExecutor;
    }
}
