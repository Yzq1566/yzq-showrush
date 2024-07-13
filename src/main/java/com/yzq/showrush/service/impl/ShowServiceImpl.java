package com.yzq.showrush.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.Show;
import com.yzq.showrush.mapper.ShowMapper;
import com.yzq.showrush.service.IShowService;
import com.yzq.showrush.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 演出服务实现：详情走缓存，更新时先写库再删缓存
 *
 * @author yzq
 * @since 2024-07-13
 */
@Service
public class ShowServiceImpl extends ServiceImpl<ShowMapper, Show> implements IShowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        String key = RedisConstants.CACHE_SHOW_KEY + id;
        // 1. 先查缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return Result.ok(JSONUtil.toBean(json, Show.class));
        }
        // 2. 未命中则查数据库
        Show show = getById(id);
        if (show == null) {
            return Result.fail("演出不存在");
        }
        // 3. 写入缓存
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(show),
                RedisConstants.CACHE_SHOW_TTL, TimeUnit.MINUTES);
        return Result.ok(show);
    }

    @Override
    @Transactional
    public Result updateShow(Show show) {
        Long id = show.getId();
        if (id == null) {
            return Result.fail("演出id不能为空");
        }
        // 先更新数据库，再删除缓存
        updateById(show);
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOW_KEY + id);
        return Result.ok();
    }
}
