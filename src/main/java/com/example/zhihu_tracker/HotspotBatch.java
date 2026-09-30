package com.example.zhihu_tracker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HotspotBatch(
        String platform,
        LocalDate date,
        int hour,
        LocalDateTime capturedAt,
        String source,
        List<HotspotItem> items
) {
}
