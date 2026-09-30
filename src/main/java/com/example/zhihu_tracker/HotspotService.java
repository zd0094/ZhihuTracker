package com.example.zhihu_tracker;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class HotspotService {
    private final ZhihuHotspotClient client;
    private final TiebaHotspotClient tiebaClient;
    private final HotspotStorage storage;
    private final ZoneId zoneId;

    public HotspotService(ZhihuHotspotClient client, TiebaHotspotClient tiebaClient, HotspotStorage storage,
                          @Value("${zhihu.time-zone:Asia/Tokyo}") String timeZone) {
        this.client = client;
        this.tiebaClient = tiebaClient;
        this.storage = storage;
        this.zoneId = ZoneId.of(timeZone);
    }

    public HotspotBatch captureScheduledSlot(String platform, LocalDate date, int hour) {
        return capture(platform, date, hour, "scheduled");
    }

    public HotspotBatch captureNextSlot(String platform) {
        LocalDateTime next = nextSlot(LocalDateTime.now(zoneId));
        return capture(platform, next.toLocalDate(), next.getHour(), "manual");
    }

    public List<HotspotBatch> findDay(LocalDate date, String platform) {
        return storage.findDay(date, platform);
    }

    public HotspotBatch findSlot(LocalDate date, int hour, String platform) {
        return storage.findSlot(date, hour, platform);
    }

    static LocalDateTime nextSlot(LocalDateTime now) {
        LocalDateTime candidate = now.toLocalDate().atTime(((now.getHour() / 2) + 1) * 2 % 24, 0);
        if (now.getHour() >= 22) candidate = now.toLocalDate().plusDays(1).atStartOfDay();
        return candidate;
    }

    private synchronized HotspotBatch capture(String platform, LocalDate date, int hour, String source) {
        List<HotspotItem> items = switch (platform) {
            case "zhihu" -> client.fetchHotspots();
            case "tieba" -> tiebaClient.fetchHotspots();
            default -> throw new IllegalArgumentException("不支持的平台：" + platform);
        };
        HotspotBatch batch = new HotspotBatch(platform, date, hour, LocalDateTime.now(zoneId), source, items);
        storage.save(batch);
        return batch;
    }
}
