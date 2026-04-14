package com.personal.cinemabooking.service;

import com.personal.cinemabooking.dto.SeatDTO;
import com.personal.cinemabooking.entity.Seat;
import com.personal.cinemabooking.entity.Showtime;
import com.personal.cinemabooking.entity.Theater;
import com.personal.cinemabooking.repo.SeatRepository;
import com.personal.cinemabooking.repo.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private SeatService seatService;

    private Showtime showtime;
    private Theater theater;
    private Seat seat1;

    @BeforeEach
    void setUp() {
        theater = new Theater();
        theater.setId(1L);
        theater.setName("Cineplex");
        theater.setCapacity(150);

        showtime = new Showtime();
        showtime.setId(1L);
        showtime.setTheater(theater);
        showtime.setTotalSeats(150);

        seat1 = new Seat();
        seat1.setId(1L);
        seat1.setSeatNumber("A1");
        seat1.setShowtime(showtime);
        seat1.setIsReserved(false);
    }

    @Test
    void testGetSeatsByShowtime_ExistingSeats() {
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByShowtime(showtime)).thenReturn(Arrays.asList(seat1));

        List<SeatDTO> result = seatService.getSeatsByShowtime(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("A1", result.get(0).getSeatNumber());
        verify(seatRepository, times(1)).findByShowtime(showtime);
    }

    @Test
    void testGetAvailableSeatsByShowtime() {
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByShowtime(showtime)).thenReturn(Arrays.asList(seat1));
        when(seatRepository.findByShowtimeAndIsReserved(showtime, false)).thenReturn(Arrays.asList(seat1));

        List<SeatDTO> result = seatService.getAvailableSeatsByShowtime(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsReserved());
    }

    @Test
    void testCreateSeatsForShowtime() {
        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        List<Seat> result = seatService.createSeatsForShowtime(showtime);

        assertNotNull(result);
        assertEquals(150, result.size()); // Cineplex layout A-O (15) x 10 seats = 150
        assertEquals("A1", result.get(0).getSeatNumber());
        verify(seatRepository, times(1)).saveAll(anyList());
    }
}
