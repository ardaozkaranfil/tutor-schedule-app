package com.tutorschedule.app.repository;

import com.tutorschedule.app.entity.TimeSlot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalTime;
import java.util.List;

import static com.tutorschedule.app.entity.TimeSlotDayType.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TimeSlotRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    void findByDayTypeOrderByStartTimeAsc_returnsOnlyMatchingDayType() {
        timeSlotRepository.save(new TimeSlot(WEEKEND, LocalTime.of(14, 0), LocalTime.of(16, 0)));
        timeSlotRepository.save(new TimeSlot(WEEKEND, LocalTime.of(17, 0), LocalTime.of(18, 0)));
        timeSlotRepository.save(new TimeSlot(WEEKDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)));

        List<TimeSlot> result = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(WEEKEND);

        assertThat(result).hasSize(2)
                .extracting(TimeSlot::getDayType)
                .containsOnly(WEEKEND);
    }

    @Test
    void findByDayTypeOrderByStartTimeAsc_ordersByStartTime() {
        timeSlotRepository.save(new TimeSlot(WEEKEND, LocalTime.of(17, 0), LocalTime.of(18, 0)));
        timeSlotRepository.save(new TimeSlot(WEEKEND, LocalTime.of(14, 0), LocalTime.of(16, 0)));
        timeSlotRepository.save(new TimeSlot(WEEKDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)));

        List<TimeSlot> result = timeSlotRepository.findByDayTypeOrderByStartTimeAsc(WEEKEND);

        assertThat(result).hasSize(2)
                .extracting(TimeSlot::getStartTime)
                .containsExactly(LocalTime.of(14, 0), LocalTime.of(17, 0));
    }
}
