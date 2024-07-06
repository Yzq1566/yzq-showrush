package com.yzq.showrush.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yzq.showrush.dto.LoginFormDTO;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.User;

/**
 * 用户服务
 *
 * @author yzq
 * @since 2024-07-06
 */
public interface IUserService extends IService<User> {

    /**
     * 发送短信验证码
     */
    Result sendCode(String phone);

    /**
     * 验证码登录，用户不存在则自动注册
     */
    Result login(LoginFormDTO loginForm);
}
