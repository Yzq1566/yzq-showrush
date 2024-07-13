package com.yzq.showrush.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.ShowCategory;
import com.yzq.showrush.mapper.ShowCategoryMapper;
import com.yzq.showrush.service.IShowCategoryService;
import com.yzq.showrush.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 演出分类服务实现：分类数据基本不变，整表放入 Redis List 缓存
 *
 * @author yzq
 * @since 2024-07-13
 */
@Service
public class ShowCategoryServiceImpl extends ServiceImpl<ShowCategoryMapper, ShowCategory> implements IShowCategoryService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryCategoryList() {
        // 1. 先查缓存
        List<String> cached = stringRedisTemplate.opsForList().range(RedisConstants.CACHE_CATEGORY_KEY, 0, -1);
        if (cached != null && !cached.isEmpty()) {
            List<ShowCategory> list = cached.stream()
                    .map(json -> JSONUtil.toBean(json, ShowCategory.class))
                    .collect(Collectors.toList());
            return Result.ok(list);
        }

        // 2. 缓存未命中，查数据库
        List<ShowCategory> list = query().orderByAsc("sort").list();
        if (list == null || list.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 3. 回填缓存：先批量写入 List，再整体设置过期时间
        List<String> jsonList = list.stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(RedisConstants.CACHE_CATEGORY_KEY, jsonList);
        stringRedisTemplate.expire(RedisConstants.CACHE_CATEGORY_KEY,
                RedisConstants.CACHE_CATEGORY_TTL, TimeUnit.MINUTES);
        return Result.ok(list);
    }
}
