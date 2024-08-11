package com.yzq.showrush.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.SeckillTicket;
import com.yzq.showrush.entity.TicketOrder;
import com.yzq.showrush.mapper.TicketOrderMapper;
import com.yzq.showrush.service.ISeckillTicketService;
import com.yzq.showrush.service.ITicketOrderService;
import com.yzq.showrush.utils.RedisIdWorker;
import com.yzq.showrush.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 秒杀下单服务实现：乐观锁防止超卖，Redisson 分布式锁保证一人一单
 *
 * @author yzq
 * @since 2024-08-03
 */
@Slf4j
@Service
public class TicketOrderServiceImpl extends ServiceImpl<TicketOrderMapper, TicketOrder> implements ITicketOrderService {

    @Resource
    private ISeckillTicketService seckillTicketService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result seckillTicket(Long ticketId) {
        // 1. 查询秒杀票
        SeckillTicket ticket = seckillTicketService.getById(ticketId);
        // 2. 判断秒杀是否开始
        if (ticket.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        // 3. 判断秒杀是否结束
        if (ticket.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        // 4. 判断库存是否充足
        if (ticket.getStock() < 1) {
            return Result.fail("库存不足");
        }
        // 5. 按用户维度加分布式锁，Redisson 自带看门狗续期，比自研锁更可靠
        Long userId = UserHolder.getUser().getId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        if (!lock.tryLock()) {
            return Result.fail("请勿重复下单");
        }
        try {
            // 获取事务代理对象，避免自调用导致事务失效
            ITicketOrderService proxy = (ITicketOrderService) AopContext.currentProxy();
            return proxy.createTicketOrder(ticketId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public Result createTicketOrder(Long ticketId) {
        Long userId = UserHolder.getUser().getId();
        // 一人一单校验
        int count = query().eq("user_id", userId).eq("ticket_id", ticketId).count();
        if (count > 0) {
            return Result.fail("每人限购一张");
        }
        // 乐观锁扣减库存：stock > 0 才能扣成功，防止超卖
        boolean success = seckillTicketService.update()
                .setSql("stock = stock - 1")
                .eq("ticket_id", ticketId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }
        // 保存订单
        TicketOrder order = new TicketOrder();
        order.setId(redisIdWorker.nextId("order"));
        order.setUserId(userId);
        order.setTicketId(ticketId);
        save(order);
        return Result.ok(order.getId());
    }
}
