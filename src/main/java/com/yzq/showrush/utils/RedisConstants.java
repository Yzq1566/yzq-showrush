package com.yzq.showrush.utils;

/**
 * Redis Key 与过期时间常量
 *
 * @author yzq
 * @since 2024-07-06
 */
public class RedisConstants {

    /** 短信验证码，Key 后缀为手机号 */
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final long LOGIN_CODE_TTL = 2L;

    /** 登录令牌，Key 后缀为 token */
    public static final String TOKEN_KEY = "login:token:";
    public static final long TOKEN_TTL = 60L;

    /** 空值缓存（防止缓存穿透），过期时间不宜过长 */
    public static final long CACHE_NULL_TTL = 2L;

    /** 演出信息缓存 */
    public static final String CACHE_SHOW_KEY = "cache:show:";
    public static final long CACHE_SHOW_TTL = 30L;

    /** 演出分类列表缓存 */
    public static final String CACHE_CATEGORY_KEY = "cache:showCategoryList";
    public static final long CACHE_CATEGORY_TTL = 60L;

    /** 热点数据缓存重建的互斥锁 */
    public static final String CACHE_LOCK_SHOW_KEY = "cache:lock:show:";
    public static final long CACHE_LOCK_TTL = 10L;

    /** 秒杀库存，Key 后缀为票档id */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";

    /** 动态点赞集合（有序集合，score 为点赞时间戳） */
    public static final String POST_LIKED_KEY = "post:liked:";

    /** 用户关注集合 */
    public static final String FOLLOW_KEY = "follow:";
}
