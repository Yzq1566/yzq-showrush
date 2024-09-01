package com.yzq.showrush.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.Follow;

/**
 * 关注服务
 *
 * @author yzq
 * @since 2024-09-01
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注或取关
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 是否已关注
     */
    Result isFollow(Long followUserId);

    /**
     * 共同关注列表
     */
    Result followCommons(Long id);
}
