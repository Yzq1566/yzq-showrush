package com.yzq.showrush.service;

import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.Ticket;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 票券服务
 *
 * @author yzq
 * @since 2024-07-27
 */
public interface ITicketService extends IService<Ticket> {

    Result queryTicketOfShow(Long showId);

    void addSeckillTicket(Ticket ticket);
}
