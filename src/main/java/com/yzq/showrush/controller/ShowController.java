package com.yzq.showrush.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.Show;
import com.yzq.showrush.service.IShowService;
import com.yzq.showrush.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 演出接口
 *
 * @author yzq
 * @since 2024-07-13
 */
@RestController
@RequestMapping("/show")
public class ShowController {

    @Resource
    public IShowService showService;

    /**
     * 根据id查询演出信息
     * @param id 演出id
     * @return 演出详情数据
     */
    @GetMapping("/{id}")
    public Result queryShowById(@PathVariable("id") Long id) {
        return showService.queryById(id);
    }

    /**
     * 新增演出信息
     * @param show 演出数据
     * @return 演出id
     */
    @PostMapping
    public Result saveShow(@RequestBody Show show) {
        // 写入数据库
        showService.save(show);
        // 返回演出id
        return Result.ok(show.getId());
    }

    /**
     * 更新演出信息
     * @param show 演出数据
     * @return 无
     */
    @PutMapping
    public Result updateShow(@RequestBody Show show) {
        return showService.updateShow(show);
    }

    /**
     * 根据演出类型分页查询演出信息
     * @param categoryId 演出类型
     * @param current 页码
     * @return 演出列表
     */
    @GetMapping("/of/type")
    public Result queryShowByCategory(
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Show> page = showService.query()
                .eq("category_id", categoryId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

    /**
     * 根据演出名称关键字分页查询演出信息
     * @param name 演出名称关键字
     * @param current 页码
     * @return 演出列表
     */
    @GetMapping("/of/name")
    public Result queryShowByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Show> page = showService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }
}
