package com.yzq.showrush.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzq.showrush.dto.Result;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 秒杀下单服务实现：Lua 脚本原子完成资格判断与扣库存，
 * 下单消息写入 Redis Stream 削峰，由后台线程异步落库
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
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    /** 订单 Stream 名称与消费者组 */
    private static final String STREAM_NAME = "stream:ticket:orders";
    private static final String GROUP_NAME = "ticket-order-group";
    private static final String CONSUMER_NAME = "c1";

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /** 订单异步处理线程 */
    private static final ExecutorService ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    /** 当前代理对象，异步线程中调用事务方法要用它 */
    private ITicketOrderService proxy;

    @PostConstruct
    private void init() {
        // 启动时确保 Stream 与消费者组存在，然后启动订单处理线程
        createStreamAndGroup();
        ORDER_EXECUTOR.submit(new TicketOrderHandler());
    }

    private void createStreamAndGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_NAME, GROUP_NAME);
        } catch (Exception e) {
            // 消费者组已存在时忽略
            log.debug("消费者组可能已存在: {}", e.getMessage());
        }
    }

    /**
     * 订单处理线程：阻塞读取 Stream 中的下单消息，处理完成后 ACK；
     * 处理失败的消息进入 pending-list，由 handlePendingList 兜底重试
     */
    private class TicketOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            StreamReadOptions.empty().count(1).block(Duration.ofMinutes(2)),
                            StreamOffset.create(STREAM_NAME, ReadOffset.lastConsumed())
                    );
                    if (list == null || list.isEmpty()) {
                        continue;
                    }
                    MapRecord<String, Object, Object> record = list.get(0);
                    TicketOrder order = BeanUtil.fillBeanWithMap(record.getValue(), new TicketOrder(), true);
                    handleTicketOrder(order);
                    stringRedisTemplate.opsForStream().acknowledge(STREAM_NAME, GROUP_NAME, record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }
    }

    /** 兜底处理 pending-list 中未确认的消息 */
    private void handlePendingList() {
        while (true) {
            try {
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(STREAM_NAME, ReadOffset.from("0"))
                );
                if (list == null || list.isEmpty()) {
                    break;
                }
                MapRecord<String, Object, Object> record = list.get(0);
                TicketOrder order = BeanUtil.fillBeanWithMap(record.getValue(), new TicketOrder(), true);
                handleTicketOrder(order);
                stringRedisTemplate.opsForStream().acknowledge(STREAM_NAME, GROUP_NAME, record.getId());
            } catch (Exception e) {
                log.error("处理 pending-list 异常", e);
                try {
                    // 发生异常时稍等再重试，避免空转
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /** 真正落库：按用户加分布式锁防止重复消费，事务内扣减库存并保存订单 */
    private void handleTicketOrder(TicketOrder order) {
        RLock lock = redissonClient.getLock("lock:order:" + order.getUserId());
        if (!lock.tryLock()) {
            log.error("重复下单，userId: {}", order.getUserId());
            return;
        }
        try {
            proxy.createTicketOrder(order);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Result seckillTicket(Long ticketId) {
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");

        // Lua 脚本原子完成：库存判断 + 防重判断 + 扣减库存 + 写入 Stream
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(),
                ticketId.toString(), userId.toString(), String.valueOf(orderId));
        int r = result.intValue();
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "请勿重复下单");
        }

        // 提前拿到代理对象，供异步线程使用
        proxy = (ITicketOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }

    @Override
    @Transactional
    public void createTicketOrder(TicketOrder order) {
        Long userId = order.getUserId();
        // 一人一单校验
        int count = query().eq("user_id", userId).eq("ticket_id", order.getTicketId()).count();
        if (count > 0) {
            log.error("用户已购买过该票档，userId: {}", userId);
            return;
        }
        // 乐观锁扣减库存：stock > 0 才能扣成功，防止超卖
        boolean success = seckillTicketService.update()
                .setSql("stock = stock - 1")
                .eq("ticket_id", order.getTicketId())
                .gt("stock", 0)
                .update();
        if (!success) {
            log.error("库存不足，ticketId: {}", order.getTicketId());
            return;
        }
        save(order);
    }
}
