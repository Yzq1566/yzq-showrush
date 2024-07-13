package com.yzq.showrush.service;

import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.Show;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 演出服务
 *
 * @author yzq
 * @since 2024-07-13
 */
public interface IShowService extends IService<Show> {

    Result queryById(Long id);

    Result updateShow(Show show);
}
