package com.ar.sheetdb.core;

import com.google.common.util.concurrent.RateLimiter;

import java.time.Duration;
import java.time.LocalDateTime;

public class BucketRateLimiter {

    private final int rate;
    private final RateLimiter rateLimiter;
    private long bucketAvailability;
    private transient final Object bucketLock;
    private LocalDateTime lastTime;
    private Duration period;

    public BucketRateLimiter(int rate, Duration period) {
        this.rate = rate;
        this.period = period;
        double numSeconds = period.getSeconds() + period.getNano() / 1000000000d;
        rateLimiter = RateLimiter.create(1 / numSeconds);
        bucketAvailability = 0L;
        bucketLock = new Object();
    }

    public void consume() {
        synchronized (bucketLock) {
            LocalDateTime current = LocalDateTime.now();
            if (lastTime != null && Duration.between(lastTime, current).toSeconds() >= this.period.toSeconds()) {
                 /* Emptying the availability as there might be already available bucket from previous period and
                 in new period again a full available bucket is provided if consumed all at once will exceed the
                 rate limit.*/
                bucketAvailability = 0;
            }
            if (bucketAvailability == 0) {
                // then refill
                rateLimiter.acquire();
                bucketAvailability = rate;
            }
            lastTime = LocalDateTime.now();
            bucketAvailability--;
        }
    }
}