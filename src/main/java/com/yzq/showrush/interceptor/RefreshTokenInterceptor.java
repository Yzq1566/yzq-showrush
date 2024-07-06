package com.yzq.showrush.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.yzq.showrush.dto.UserDTO;
import com.yzq.showrush.utils.RedisConstants;
import com.yzq.showrush.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 令牌续期拦截器：拦截所有请求，只要携带有效 token 就刷新 Redis 中登录状态的有效期，
 * 并把用户信息存入 ThreadLocal 供后续使用
 *
 * @author yzq
 * @since 2024-07-06
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 未携带 token，放行（由后面的登录校验拦截器决定是否拦截）
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }

        // 查询 Redis 中的登录用户
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(RedisConstants.TOKEN_KEY + token);
        if (map.isEmpty()) {
            return true;
        }

        // 存入 ThreadLocal 并刷新有效期
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        stringRedisTemplate.expire(RedisConstants.TOKEN_KEY + token, RedisConstants.TOKEN_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
