package com.yzq.showrush.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.SeckillTicket;
import com.yzq.showrush.entity.Ticket;
import com.yzq.showrush.mapper.TicketMapper;
import com.yzq.showrush.service.ISeckillTicketService;
import com.yzq.showrush.service.ITicketService;
import com.yzq.showrush.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 票档服务实现
 *
 * @author yzq
 * @since 2024-07-27
 */
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements ITicketService {

    @Resource
    private ISeckillTicketService seckillTicketService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTicketOfShow(Long showId) {
        // 查询票档信息（左连接秒杀票，带上库存与秒杀时间）
        List<Ticket> tickets = getBaseMapper().queryTicketOfShow(showId);
        return Result.ok(tickets);
    }

    @Override
    @Transactional
    public void addSeckillTicket(Ticket ticket) {
        // 保存票档
        save(ticket);
        // 保存秒杀信息
        SeckillTicket seckillTicket = new SeckillTicket();
        seckillTicket.setTicketId(ticket.getId());
        seckillTicket.setStock(ticket.getStock());
        seckillTicket.setBeginTime(ticket.getBeginTime());
        seckillTicket.setEndTime(ticket.getEndTime());
        seckillTicketService.save(seckillTicket);
        // 库存预热到 Redis，秒杀时直接在 Redis 中扣减，不经过数据库
        stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + ticket.getId(),
                ticket.getStock().toString());
    }
}
