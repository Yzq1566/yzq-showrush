package com.yzq.showrush.config;

import com.yzq.showrush.interceptor.LoginInterceptor;
import com.yzq.showrush.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * MVC 配置：注册令牌续期与登录校验拦截器
 *
 * @author yzq
 * @since 2024-07-06
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 令牌续期拦截所有请求，先执行
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**").order(0);
        // 登录校验只拦需要登录的接口
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/login",
                        "/user/code",
                        "/post/hot",
                        "/upload/**",
                        "/show-category/**",
                        "/show/**",
                        "/ticket/**"
                ).order(1);
    }
}
