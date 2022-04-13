package com.ar.sheetdb;

import com.google.common.util.concurrent.RateLimiter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.concurrent.TimeUnit;

public class BucketRateLimiter {

    private final int numDosesPerPeriod;
    private final RateLimiter rateLimiter;
    private long numDosesAvailable;
    private transient final Object doseLock;
    private LocalDateTime lastTime;
    private Duration period;

    public BucketRateLimiter(int numDosesPerPeriod, Duration period) {
        this.numDosesPerPeriod = numDosesPerPeriod;
        this.period = period;
        double numSeconds = period.getSeconds() + period.getNano() / 1000000000d;
        rateLimiter = RateLimiter.create(1 / numSeconds);
        numDosesAvailable = 0L;
        doseLock = new Object();
    }

    /**
     * Consumes a dose from this titrator, blocking until a dose is available.
     */
    public void consume() {
        synchronized (doseLock) {
            LocalDateTime current = LocalDateTime.now();
            if(lastTime!= null && Duration.between(lastTime, current).toSeconds() >= this.period.toSeconds()){
                numDosesAvailable=0;
            }
            if (numDosesAvailable == 0) { // then refill
                rateLimiter.acquire();
                numDosesAvailable = numDosesPerPeriod;
            }
            lastTime = LocalDateTime.now();
            numDosesAvailable--;
        }
    }

}