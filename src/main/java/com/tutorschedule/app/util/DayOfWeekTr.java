package com.tutorschedule.app.util;

import java.time.DayOfWeek;
import java.util.Map;

public final class DayOfWeekTr {
    private static final Map<DayOfWeek, String> NAMES = Map.of(
            DayOfWeek.MONDAY, "Pazartesi",
            DayOfWeek.TUESDAY, "Salı",
            DayOfWeek.WEDNESDAY, "Çarşamba",
            DayOfWeek.THURSDAY, "Perşembe",
            DayOfWeek.FRIDAY, "Cuma",
            DayOfWeek.SATURDAY, "Cumartesi",
            DayOfWeek.SUNDAY, "Pazar"
    );
    public static String of(DayOfWeek day) { return NAMES.get(day); }
}