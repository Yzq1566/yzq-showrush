package com.yzq.showrush.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.PostComment;
import com.yzq.showrush.service.IPostCommentService;
import com.yzq.showrush.utils.SystemConstants;
import com.yzq.showrush.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 动态评论接口
 *
 * @author yzq
 * @since 2024-09-01
 */
@RestController
@RequestMapping("/post-comments")
public class PostCommentController {

    @Resource
    private IPostCommentService postCommentService;

    /**
     * 发表评论
     */
    @PostMapping
    public Result saveComment(@RequestBody PostComment comment) {
        comment.setUserId(UserHolder.getUser().getId());
        comment.setLiked(0);
        postCommentService.save(comment);
        return Result.ok(comment.getId());
    }

    /**
     * 按动态分页查询评论，按点赞数倒序
     */
    @GetMapping("/of/post/{id}")
    public Result queryByPost(@PathVariable("id") Long postId,
                              @RequestParam(value = "current", defaultValue = "1") Integer current) {
        Page<PostComment> page = postCommentService.query()
                .eq("post_id", postId)
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }
}
