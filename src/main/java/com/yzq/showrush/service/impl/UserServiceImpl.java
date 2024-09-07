package com.yzq.showrush.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzq.showrush.dto.LoginFormDTO;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.dto.UserDTO;
import com.yzq.showrush.entity.User;
import com.yzq.showrush.mapper.UserMapper;
import com.yzq.showrush.service.IUserService;
import com.yzq.showrush.utils.RedisConstants;
import com.yzq.showrush.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.yzq.showrush.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 用户服务实现：验证码登录，登录状态保存在 Redis，过期自动续期
 *
 * @author yzq
 * @since 2024-07-06
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone) {
        // 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 生成验证码并写入 Redis，过期时间 2 分钟
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code,
                RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 本地开发没有接短信平台，先打到日志里
        log.debug("发送验证码: {}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 校验验证码
        String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (code == null || !code.equals(redisCode)) {
            return Result.fail("验证码错误");
        }

        // 用户不存在则自动注册
        User user = query().eq("phone", phone).one();
        if (user == null) {
            user = createUserByPhone(phone);
        }

        // 生成 token，用户信息以 Hash 结构存入 Redis
        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(RedisConstants.TOKEN_KEY + token, map);
        stringRedisTemplate.expire(RedisConstants.TOKEN_KEY + token, RedisConstants.TOKEN_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    @Override
    public Result logout(String token) {
        if (StrUtil.isNotBlank(token)) {
            stringRedisTemplate.delete(RedisConstants.TOKEN_KEY + token);
        }
        return Result.ok();
    }

    private User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
