package com.yzq.showrush.service;

import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.ShowCategory;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 演出分类服务
 *
 * @author yzq
 * @since 2024-07-13
 */
public interface IShowCategoryService extends IService<ShowCategory> {

    Result queryCategoryList();
}
