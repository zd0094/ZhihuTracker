package com.example.zhihu_tracker;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class HotspotStorage {
    private final Path dataDirectory;
    private final JsonMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();
    private final ConcurrentHashMap<Integer, Object> yearLocks = new ConcurrentHashMap<>();

    public HotspotStorage(@Value("${zhihu.data-dir:./data}") String dataDirectory) {
        this.dataDirectory = Path.of(dataDirectory).toAbsolutePath().normalize();
    }

    public synchronized void save(HotspotBatch batch) {
        Object lock = yearLocks.computeIfAbsent(batch.date().getYear(), ignored -> new Object());
        synchronized (lock) {
            try {
                Files.createDirectories(dataDirectory);
                Path file = yearFile(batch.date().getYear());
                List<HotspotBatch> batches = readYear(file);
                batches.removeIf(existing -> existing.date().equals(batch.date())
                        && existing.hour() == batch.hour() && samePlatform(existing.platform(), batch.platform()));
                batches.add(batch);
                batches.sort((left, right) -> {
                    int dateOrder = left.date().compareTo(right.date());
                    return dateOrder != 0 ? dateOrder : Integer.compare(left.hour(), right.hour());
                });
                Path temporary = Files.createTempFile(dataDirectory, "hotspots-", ".tmp");
                try {
                    mapper.writeValue(temporary.toFile(), batches);
                    try {
                        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException ignored) {
                        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(temporary);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("无法写入知乎热点年度数据", exception);
            }
        }
    }

    public List<HotspotBatch> findDay(LocalDate date, String platform) {
        Object lock = yearLocks.computeIfAbsent(date.getYear(), ignored -> new Object());
        synchronized (lock) {
            List<HotspotBatch> batches = readYear(yearFile(date.getYear()));
            return batches.stream().filter(batch -> batch.date().equals(date)
                    && samePlatform(batch.platform(), platform)).toList();
        }
    }

    public HotspotBatch findSlot(LocalDate date, int hour, String platform) {
        return findDay(date, platform).stream().filter(batch -> batch.hour() == hour).findFirst().orElse(null);
    }

    private static boolean samePlatform(String stored, String requested) {
        return (stored == null ? "zhihu" : stored).equals(requested);
    }

    private Path yearFile(int year) {
        return dataDirectory.resolve(year + ".json");
    }

    private List<HotspotBatch> readYear(Path file) {
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            return mapper.readValue(file.toFile(), new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("无法读取知乎热点年度数据：" + file.getFileName(), exception);
        }
    }
}
