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
     * 秒杀下单
     */
    Result seckillTicket(Long ticketId);

    /**
     * 创建订单（事务方法，通过代理对象调用）
     */
    Result createTicketOrder(Long ticketId);
}
