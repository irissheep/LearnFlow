package com.learnflow.stats;
import java.time.Duration;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class StatsCache {
    private static final Logger log = LoggerFactory.getLogger(StatsCache.class);
    private final StringRedisTemplate redis;
    private final boolean enabled;
    public StatsCache(StringRedisTemplate redis, @Value("${learnflow.cache-enabled:false}") boolean enabled) {
        this.redis = redis; this.enabled = enabled;
    }
    public TaskCounts get(long userId) {
        if (!enabled) return null;
        try {
            String value = redis.opsForValue().get(key(userId));
            if (value == null) return null;
            String[] fields = value.split(",");
            return new TaskCounts(Long.parseLong(fields[0]), Long.parseLong(fields[1]), Long.parseLong(fields[2]));
        } catch (RuntimeException ex) { log.debug("Stats cache unavailable"); return null; }
    }
    public void put(long userId, TaskCounts counts) {
        if (!enabled) return;
        try {
            redis.opsForValue().set(key(userId), counts.total()+","+counts.done()+","+counts.inProgress(), Duration.ofSeconds(60));
        } catch (RuntimeException ex) { log.debug("Stats cache unavailable"); }
    }
    // 只在 MySQL 提交成功后失效缓存，避免事务回滚或提前清缓存导致错误统计。
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evict(TaskChanged event) {
        if (!enabled) return;
        try { redis.delete(key(event.userId())); }
        catch (RuntimeException ex) { log.debug("Stats cache eviction unavailable; TTL bounds staleness"); }
    }
    private String key(long userId) { return "learnflow:stats:" + userId; }
}
