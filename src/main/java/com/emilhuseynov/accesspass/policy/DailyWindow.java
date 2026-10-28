package com.emilhuseynov.accesspass.policy;

import java.time.LocalTime;
import java.util.Objects;

public record DailyWindow(LocalTime start, LocalTime end) {

    public DailyWindow {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.equals(end)) {
            throw new IllegalArgumentException("Window start and end must differ");
        }
    }

    public boolean contains(LocalTime time) {
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }
}
