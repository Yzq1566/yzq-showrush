package com.yzq.showrush.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yzq.showrush.entity.Ticket;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 票券 Mapper
 *
 * @author yzq
 * @since 2024-07-27
 */
public interface TicketMapper extends BaseMapper<Ticket> {

    List<Ticket> queryTicketOfShow(@Param("showId") Long showId);
}
