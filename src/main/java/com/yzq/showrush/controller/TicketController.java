package com.yzq.showrush.controller;


import com.yzq.showrush.dto.Result;
import com.yzq.showrush.entity.Ticket;
import com.yzq.showrush.service.ITicketService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 票券接口
 *
 * @author yzq
 * @since 2024-07-27
 */
@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Resource
    private ITicketService ticketService;

    /**
     * 新增普通票
     * @param ticket 票券信息
     * @return 票券id
     */
    @PostMapping
    public Result addTicket(@RequestBody Ticket ticket) {
        ticketService.save(ticket);
        return Result.ok(ticket.getId());
    }

    /**
     * 新增秒杀票
     * @param ticket 票券信息，包含秒杀信息
     * @return 票券id
     */
    @PostMapping("seckill")
    public Result addSeckillTicket(@RequestBody Ticket ticket) {
        ticketService.addSeckillTicket(ticket);
        return Result.ok(ticket.getId());
    }

    /**
     * 查询演出的票券列表
     * @param showId 演出id
     * @return 票券列表
     */
    @GetMapping("/list/{showId}")
    public Result queryTicketOfShow(@PathVariable("showId") Long showId) {
       return ticketService.queryTicketOfShow(showId);
    }
}
