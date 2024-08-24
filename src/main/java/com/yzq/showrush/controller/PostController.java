package com.yzq.showrush.controller;

import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.Post;
import com.yzq.showrush.service.IPostService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 观演动态接口
 *
 * @author yzq
 * @since 2024-08-24
 */
@RestController
@RequestMapping("/post")
public class PostController {

    @Resource
    private IPostService postService;

    /**
     * 发布动态
     */
    @PostMapping
    public Result savePost(@RequestBody Post post) {
        return postService.savePost(post);
    }

    /**
     * 点赞或取消点赞
     */
    @PutMapping("/like/{id}")
    public Result likePost(@PathVariable("id") Long id) {
        return postService.likePost(id);
    }

    /**
     * 我的动态列表
     */
    @GetMapping("/of/me")
    public Result queryMyPost(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return postService.queryMyPost(current);
    }

    /**
     * 热门动态列表
     */
    @GetMapping("/hot")
    public Result queryHotPost(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return postService.queryHotPost(current);
    }
}
