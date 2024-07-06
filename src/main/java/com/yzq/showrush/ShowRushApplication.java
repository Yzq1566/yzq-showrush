package com.yzq.showrush;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * ShowRush 演出票务秒杀平台启动类
 */
@MapperScan("com.yzq.showrush.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 暴露代理对象，异步线程中通过 AopContext 调用事务方法
public class ShowRushApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShowRushApplication.class, args);
    }
}
