package com.yzq.showrush;

import com.yzq.showrush.entity.Show;
import com.yzq.showrush.service.impl.ShowServiceImpl;
import com.yzq.showrush.utils.CacheClient;
import com.yzq.showrush.utils.RedisConstants;
import com.yzq.showrush.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class ShowRushApplicationTests {

    @Resource
    private CacheClient cacheClient;

    @Resource
    private ShowServiceImpl showService;

    @Resource
    private RedisIdWorker redisIdWorker;

    /**
     * 热点演出缓存预热：写入带逻辑过期时间的数据
     */
    @Test
    void testCacheWarmUp() {
        Show show = showService.getById(1L);
        cacheClient.setWithLogicalExpire(RedisConstants.CACHE_SHOW_KEY + 1L, show, 10L, TimeUnit.SECONDS);
    }

    /**
     * ID 生成器压测：300 个线程各自生成 100 个 ID
     */
    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        ExecutorService executor = Executors.newFixedThreadPool(300);
        for (int i = 0; i < 300; i++) {
            executor.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    long id = redisIdWorker.nextId("order");
                    System.out.println("id = " + id);
                }
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();
    }
}
