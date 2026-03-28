package com.wallet.controller.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TransactionDateTimeFormatter {

    public static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DISPLAY_ZONE);

    private TransactionDateTimeFormatter() {
    }

    public static String format(Instant instant) {
        return FORMATTER.format(instant);
    }
}
