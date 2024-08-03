package com.yzq.showrush.controller;


import com.yzq.showrush.dto.Result;
import com.yzq.showrush.service.ITicketOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀订单接口
 *
 * @author yzq
 * @since 2024-08-03
 */
@RestController
@RequestMapping("/ticket-order")
public class TicketOrderController {

    @Autowired
    private ITicketOrderService ticketOrderService;


    @PostMapping("seckill/{id}")
    public Result seckillTicket(@PathVariable("id") Long ticketId) {
        return ticketOrderService.seckillTicket(ticketId);
    }
}
