-- 秒杀资格判断与下单消息写入（脚本整体原子执行，避免超卖与重复下单）
-- ARGV[1]: 票档id  ARGV[2]: 用户id  ARGV[3]: 订单id
local ticketId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

-- 库存 Key 与下单记录 Key
local stockKey = "seckill:stock:" .. ticketId
local orderKey = "seckill:order:" .. ticketId

-- 1. 库存不足，返回 1
if tonumber(redis.call("get", stockKey)) <= 0 then
    return 1
end

-- 2. 用户已下单过，返回 2
if redis.call("sismember", orderKey, userId) == 1 then
    return 2
end

-- 3. 扣减库存并记录下单用户
redis.call("incrby", stockKey, -1)
redis.call("sadd", orderKey, userId)

-- 4. 下单信息写入 Stream，由后台线程异步落库
redis.call("xadd", "stream:ticket:orders", "*", "userId", userId, "ticketId", ticketId, "id", orderId)

return 0
