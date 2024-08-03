-- 释放分布式锁：先比对锁中保存的线程标识，一致才删除，防止误删其他线程的锁
if (redis.call("get", KEYS[1]) == ARGV[1]) then
    return redis.call("del", KEYS[1])
end
return 0
