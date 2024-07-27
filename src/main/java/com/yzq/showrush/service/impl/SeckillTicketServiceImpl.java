package com.yzq.showrush.service.impl;

import com.yzq.showrush.entity.SeckillTicket;
import com.yzq.showrush.mapper.SeckillTicketMapper;
import com.yzq.showrush.service.ISeckillTicketService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 秒杀票表，与票券是一对一关系
 *
 * @author yzq
 * @since 2024-07-27
 */
@Service
public class SeckillTicketServiceImpl extends ServiceImpl<SeckillTicketMapper, SeckillTicket> implements ISeckillTicketService {

}
