package com.yzq.showrush.service.impl;

import com.yzq.showrush.entity.UserInfo;
import com.yzq.showrush.mapper.UserInfoMapper;
import com.yzq.showrush.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务实现
 *
 * @author yzq
 * @since 2024-07-06
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
