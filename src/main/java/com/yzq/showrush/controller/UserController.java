package com.yzq.showrush.controller;

import com.yzq.showrush.dto.LoginFormDTO;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.UserInfo;
import com.yzq.showrush.service.IUserInfoService;
import com.yzq.showrush.service.IUserService;
import com.yzq.showrush.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户接口
 *
 * @author yzq
 * @since 2024-07-06
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sendCode(phone);
    }

    /**
     * 登录
     * @param loginForm 登录参数：手机号 + 验证码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm) {
        return userService.login(loginForm);
    }

    /**
     * 登出：删除 Redis 中的登录令牌
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "authorization", required = false) String token) {
        return userService.logout(token);
    }

    /**
     * 当前登录用户信息
     */
    @GetMapping("/me")
    public Result me() {
        return Result.ok(UserHolder.getUser());
    }

    /**
     * 用户详情
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }
}
