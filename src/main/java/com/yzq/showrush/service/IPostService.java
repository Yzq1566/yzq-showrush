package com.yzq.showrush.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.Post;

/**
 * 观演动态服务
 *
 * @author yzq
 * @since 2024-08-24
 */
public interface IPostService extends IService<Post> {

    /**
     * 发布动态
     */
    Result savePost(Post post);

    /**
     * 点赞或取消点赞，Redis 有序集合保证一人只能点一次
     */
    Result likePost(Long id);

    /**
     * 查询当前用户发布的动态
     */
    Result queryMyPost(Integer current);

    /**
     * 查询热门动态，附带作者信息与当前用户点赞状态
     */
    Result queryHotPost(Integer current);
}
