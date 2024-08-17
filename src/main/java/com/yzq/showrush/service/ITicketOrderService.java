package com.yzq.showrush.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.TicketOrder;

/**
 * 秒杀订单服务
 *
 * @author yzq
 * @since 2024-08-03
 */
public interface ITicketOrderService extends IService<TicketOrder> {

    /**
     * 秒杀下单：Lua 脚本完成资格判断后写入 Stream，异步落库
     */
    Result seckillTicket(Long ticketId);

    /**
     * 创建订单（事务方法，由异步线程通过代理对象调用）
     */
    void createTicketOrder(TicketOrder order);
}
