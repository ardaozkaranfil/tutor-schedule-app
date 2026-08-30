package com.tutorschedule.app.controller;

import com.tutorschedule.app.entity.TimeSlotDayType;
import com.tutorschedule.app.service.TimeSlotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeSlotController.class)
class TimeSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeSlotService timeSlotService;

    @Test
    void showSettings_returnsSettingsViewWithBothSlotLists() throws Exception {
        when(timeSlotService.getWeekdayTimeSlots()).thenReturn(List.of());
        when(timeSlotService.getWeekendTimeSlots()).thenReturn(List.of());

        mockMvc.perform(get("/timeslots"))
                .andExpect(status().isOk())
                .andExpect(view().name("timeslot/settings"))
                .andExpect(model().attributeExists("weekdaySlots"))
                .andExpect(model().attributeExists("weekendSlots"));
    }

    @Test
    void addTimeSlot_validInput_redirectsToTimeslots() throws Exception {
        mockMvc.perform(post("/timeslots/add")
                        .param("dayType", "WEEKDAY")
                        .param("startTime", "14:00")
                        .param("endTime", "14:40"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/timeslots"));

        verify(timeSlotService).addTimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14, 0), LocalTime.of(14, 40));
    }

    @Test
    void addTimeSlot_overlapping_setsFlashErrorAndRedirects() throws Exception {
        when(timeSlotService.addTimeSlot(TimeSlotDayType.WEEKDAY, LocalTime.of(14, 0), LocalTime.of(14, 40)))
                .thenThrow(new IllegalArgumentException("Bu saat mevcut bir saatle çakışıyor: 14:00 - 14:40"));

        mockMvc.perform(post("/timeslots/add")
                        .param("dayType", "WEEKDAY")
                        .param("startTime", "14:00")
                        .param("endTime", "14:40"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/timeslots"))
                .andExpect(flash().attribute("errorMessage", "Bu saat mevcut bir saatle çakışıyor: 14:00 - 14:40"));
    }

    @Test
    void editTimeSlot_validInput_redirectsToTimeslots() throws Exception {
        mockMvc.perform(post("/timeslots/edit/{id}", 1L)
                        .param("startTime", "15:00")
                        .param("endTime", "15:40"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/timeslots"));

        verify(timeSlotService).updateTimeSlot(1L, LocalTime.of(15, 0), LocalTime.of(15, 40));
    }

    @Test
    void editTimeSlot_invalidRange_setsFlashErrorAndRedirects() throws Exception {
        when(timeSlotService.updateTimeSlot(1L, LocalTime.of(15, 0), LocalTime.of(14, 0)))
                .thenThrow(new IllegalArgumentException("Bitiş saati başlangıç saatinden sonra olmalı"));

        mockMvc.perform(post("/timeslots/edit/{id}", 1L)
                        .param("startTime", "15:00")
                        .param("endTime", "14:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/timeslots"))
                .andExpect(flash().attribute("errorMessage", "Bitiş saati başlangıç saatinden sonra olmalı"));
    }

    @Test
    void deleteTimeSlot_redirectsToTimeslots() throws Exception {
        mockMvc.perform(post("/timeslots/delete/{id}", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/timeslots"));

        verify(timeSlotService).deleteTimeSlot(1L);
    }
}