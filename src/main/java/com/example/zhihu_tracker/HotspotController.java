package com.example.zhihu_tracker;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hotspots")
public class HotspotController {
    private final HotspotService service;

    public HotspotController(HotspotService service) {
        this.service = service;
    }

    @GetMapping
    public List<HotspotBatch> getDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam(defaultValue = "zhihu") String platform) {
        validatePlatform(platform);
        return service.findDay(date, platform);
    }

    @GetMapping("/slot")
    public HotspotBatch getSlot(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestParam int hour,
                                @RequestParam(defaultValue = "zhihu") String platform) {
        validateHour(hour);
        validatePlatform(platform);
        HotspotBatch batch = service.findSlot(date, hour, platform);
        if (batch == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该时段尚无已采集数据");
        return batch;
    }

    @PostMapping("/crawl")
    public HotspotBatch crawlNow(@RequestParam(defaultValue = "zhihu") String platform) {
        validatePlatform(platform);
        return service.captureNextSlot(platform);
    }

    private static void validateHour(int hour) {
        if (hour < 0 || hour > 22 || hour % 2 != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hour 必须是 0 到 22 的偶数");
        }
    }

    private static void validatePlatform(String platform) {
        if (!platform.equals("zhihu") && !platform.equals("tieba")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "platform 必须是 zhihu 或 tieba");
        }
    }
}
