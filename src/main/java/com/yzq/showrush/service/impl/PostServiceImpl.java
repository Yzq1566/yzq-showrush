package com.yzq.showrush.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.dto.UserDTO;
import com.yzq.showrush.entity.Post;
import com.yzq.showrush.entity.User;
import com.yzq.showrush.mapper.PostMapper;
import com.yzq.showrush.service.IPostService;
import com.yzq.showrush.service.IUserService;
import com.yzq.showrush.utils.RedisConstants;
import com.yzq.showrush.utils.SystemConstants;
import com.yzq.showrush.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 观演动态服务实现：点赞用 Redis 有序集合记录，score 为时间戳，支持按点赞先后排序
 *
 * @author yzq
 * @since 2024-08-24
 */
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result savePost(Post post) {
        post.setUserId(UserHolder.getUser().getId());
        save(post);
        return Result.ok(post.getId());
    }

    @Override
    public Result likePost(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.POST_LIKED_KEY + id;
        // 判断当前用户是否已点赞
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 未点赞：点赞数 +1，并记录到点赞集合
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            if (success) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 已点赞：再点一次视为取消
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if (success) {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryMyPost(Integer current) {
        UserDTO user = UserHolder.getUser();
        Page<Post> page = query()
                .eq("user_id", user.getId())
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }

    @Override
    public Result queryHotPost(Integer current) {
        Page<Post> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Post> records = page.getRecords();
        records.forEach(post -> {
            fillPostAuthor(post);
            fillPostLiked(post);
        });
        return Result.ok(records);
    }

    /** 填充动态作者的昵称与头像 */
    private void fillPostAuthor(Post post) {
        User user = userService.getById(post.getUserId());
        post.setName(user.getNickName());
        post.setIcon(user.getIcon());
    }

    /** 填充当前登录用户是否已点赞 */
    private void fillPostLiked(Post post) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 未登录不查点赞状态
            return;
        }
        String key = RedisConstants.POST_LIKED_KEY + post.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, user.getId().toString());
        post.setIsLike(score != null);
    }
}
