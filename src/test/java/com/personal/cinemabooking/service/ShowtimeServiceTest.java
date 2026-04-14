package com.personal.cinemabooking.service;

import com.personal.cinemabooking.dto.ShowtimeDTO;
import com.personal.cinemabooking.entity.Movie;
import com.personal.cinemabooking.entity.Showtime;
import com.personal.cinemabooking.entity.Theater;
import com.personal.cinemabooking.repo.MovieRepository;
import com.personal.cinemabooking.repo.SeatRepository;
import com.personal.cinemabooking.repo.ShowtimeRepository;
import com.personal.cinemabooking.repo.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShowtimeServiceTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private TheaterRepository theaterRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ShowtimeService showtimeService;

    private Showtime showtime;
    private Movie movie;
    private Theater theater;

    @BeforeEach
    void setUp() {
        movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Test Movie");

        theater = new Theater();
        theater.setId(1L);
        theater.setName("Test Theater");
        theater.setCapacity(100);

        showtime = new Showtime();
        showtime.setId(1L);
        showtime.setMovie(movie);
        showtime.setTheater(theater);
        showtime.setShowDate(LocalDate.now());
        showtime.setShowTime(LocalTime.of(18, 0));
        showtime.setPrice(10.0);
        showtime.setTotalSeats(100);
        showtime.setAvailableSeats(100);
    }

    @Test
    void testGetShowtimesByDate_Success() {
        LocalDate date = LocalDate.now();
        when(showtimeRepository.findByShowDate(date)).thenReturn(Arrays.asList(showtime));

        List<ShowtimeDTO> result = showtimeService.getShowtimesByDate(date);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(showtimeRepository, times(1)).findByShowDate(date);
    }

    @Test
    void testAddShowtime_Success() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(showtimeRepository.save(any(Showtime.class))).thenReturn(showtime);
        when(seatRepository.saveAll(anyList())).thenReturn(Arrays.asList());

        ShowtimeDTO result = showtimeService.addShowtime(showtime);

        assertNotNull(result);
        verify(showtimeRepository, times(1)).save(any(Showtime.class));
        verify(seatRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testDeleteShowtime_Success() {
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        doNothing().when(showtimeRepository).delete(showtime);

        showtimeService.deleteShowtime(1L);

        verify(showtimeRepository, times(1)).delete(showtime);
    }
}
