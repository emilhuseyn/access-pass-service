package com.emilhuseynov.accesspass.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DailyWindowTest {

    @ParameterizedTest
    @CsvSource({
            "08:00, true",
            "12:30, true",
            "17:59:59, true",
            "18:00, false",
            "07:59, false",
            "23:00, false"
    })
    void daytimeWindowIsStartInclusiveEndExclusive(LocalTime time, boolean expected) {
        DailyWindow window = new DailyWindow(LocalTime.of(8, 0), LocalTime.of(18, 0));

        assertThat(window.contains(time)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "22:00, true",
            "23:59, true",
            "00:00, true",
            "05:59, true",
            "06:00, false",
            "12:00, false",
            "21:59, false"
    })
    void overnightWindowWrapsAroundMidnight(LocalTime time, boolean expected) {
        DailyWindow window = new DailyWindow(LocalTime.of(22, 0), LocalTime.of(6, 0));

        assertThat(window.contains(time)).isEqualTo(expected);
    }

    @Test
    void rejectsEmptyWindow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DailyWindow(LocalTime.NOON, LocalTime.NOON));
    }
}
