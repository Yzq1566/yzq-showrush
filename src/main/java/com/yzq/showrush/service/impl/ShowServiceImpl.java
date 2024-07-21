package com.yzq.showrush.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.Show;
import com.yzq.showrush.mapper.ShowMapper;
import com.yzq.showrush.service.IShowService;
import com.yzq.showrush.utils.CacheClient;
import com.yzq.showrush.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 演出服务实现：详情走缓存（空值防穿透），更新时先写库再删缓存保证一致性
 *
 * @author yzq
 * @since 2024-07-13
 */
@Service
public class ShowServiceImpl extends ServiceImpl<ShowMapper, Show> implements IShowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存空值方案防穿透
        Show show = cacheClient.getWithPassThrouth(
                RedisConstants.CACHE_SHOW_KEY, id, Show.class, this::getById,
                RedisConstants.CACHE_SHOW_TTL, TimeUnit.MINUTES);

        // 热点演出可切换为逻辑过期方案（需要提前预热缓存）：
        // Show show = cacheClient.getWithLocalExpire(
        //         RedisConstants.CACHE_LOCK_SHOW_KEY, RedisConstants.CACHE_SHOW_KEY, id,
        //         Show.class, this::getById, RedisConstants.CACHE_SHOW_TTL, TimeUnit.MINUTES);

        if (show == null) {
            return Result.fail("演出不存在");
        }
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
