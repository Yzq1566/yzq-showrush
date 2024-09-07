package com.yzq.showrush.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 全局唯一 ID 生成器：高位为相对起始时间的秒数，低位为当天该业务前缀的自增序号
 *
 * @author yzq
 * @since 2024-08-03
 */
@Component
public class RedisIdWorker {

    /** 起始时间戳：2024-01-01 00:00:00 UTC */
    private static final long BEGIN_TIMESTAMP = 1704038400L;

    /** 自增序号占用的位数 */
    private static final int COUNT_BITS = 32;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成 ID，Key 自带日期，序号天然按天归零
     */
    public long nextId(String keyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        long nowSeconds = now.toEpochSecond(ZoneOffset.UTC);
        long offsetSeconds = nowSeconds - BEGIN_TIMESTAMP;

        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        return offsetSeconds << COUNT_BITS | increment;
    }
}
