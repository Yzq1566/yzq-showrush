package com.yzq.showrush.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Redis 缓存工具：封装缓存穿透、逻辑过期重建等常见场景
 *
 * @author yzq
 * @since 2024-07-21
 */
@Component
@Slf4j
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    /** 缓存重建线程池 */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 写入缓存并设置物理过期时间
     */
    public void set(String key, Object value, Long time, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    /**
     * 写入缓存并设置逻辑过期时间（用于热点数据，过期后不删 Key，由后台线程重建）
     */
    public void setWithLocalExpire(String key, Object value, Long time, TimeUnit timeUnit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 防穿透查询：命中空值标记直接返回；未命中回源数据库并回填缓存
     */
    public <T, ID> T getWithPassThrouth(String keyPrefix, ID id, Class<T> type, Function<ID, T> dbFallback, Long time, TimeUnit timeUnit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);

        // 命中正常缓存，直接返回
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        // 命中空值标记，直接返回，避免打到数据库
        if (json != null) {
            return null;
        }

        // 未命中，回源数据库
        T t = dbFallback.apply(id);
        if (t == null) {
            // 数据库也不存在：缓存空值标记
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
        }

        // 回填缓存
        this.set(key, t, time, timeUnit);
        return t;
    }

    /**
     * 逻辑过期查询：数据过期后不阻塞请求，抢到锁的线程提交后台重建任务，本次先返回旧数据
     */
    public <T, ID> T getWithLocalExpire(String lockKeyPrefix, String keyPrefix, ID id, Class<T> type, Function<ID, T> dbFallback, Long time, TimeUnit timeUnit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }

        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        T t = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            return t;
        }

        // 已过期：抢锁成功则交给后台线程重建
        String lockKey = lockKeyPrefix + id;
        if (trylock(lockKey)) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    T data = dbFallback.apply(id);
                    this.setWithLocalExpire(key, data, time, timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(lockKey);
                }
            });
        }
        return t;
    }

    private boolean trylock(String key) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.CACHE_LOCK_TTL, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
