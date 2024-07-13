package com.yzq.showrush.controller;


import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.ShowCategory;
import com.yzq.showrush.service.IShowCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 演出分类接口
 *
 * @author yzq
 * @since 2024-07-13
 */
@RestController
@RequestMapping("/show-category")
public class ShowCategoryController {
    @Resource
    private IShowCategoryService categoryService;

    @GetMapping("/list")
    public Result queryCategoryList() {
        return categoryService.queryCategoryList();
    }
}
