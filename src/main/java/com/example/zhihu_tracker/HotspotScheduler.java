package com.example.zhihu_tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class HotspotScheduler {
    private static final Logger log = LoggerFactory.getLogger(HotspotScheduler.class);
    private final HotspotService service;
    private final ZoneId zoneId;

    public HotspotScheduler(HotspotService service, @Value("${zhihu.time-zone:Asia/Tokyo}") String timeZone) {
        this.service = service;
        this.zoneId = ZoneId.of(timeZone);
    }

    @Scheduled(cron = "0 0 0/2 * * *", zone = "${zhihu.time-zone:Asia/Tokyo}")
    public void captureEveryTwoHours() {
        LocalDateTime now = LocalDateTime.now(zoneId);
        int hour = now.getHour() / 2 * 2;
        capturePlatform("zhihu", now, hour);
        capturePlatform("tieba", now, hour);
    }

    private void capturePlatform(String platform, LocalDateTime now, int hour) {
        try {
            HotspotBatch batch = service.captureScheduledSlot(platform, now.toLocalDate(), hour);
            log.info("{} 热榜已采集：{}，共 {} 条", platform, batch.date().atTime(batch.hour(), 0), batch.items().size());
        } catch (Exception exception) {
            log.error("{} 热榜定时采集失败：{}", platform, now.toLocalDate().atTime(hour, 0), exception);
        }
    }
}
