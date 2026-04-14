package com.personal.cinemabooking.service;

import com.personal.cinemabooking.dto.MasterDataDTO;
import com.personal.cinemabooking.dto.ReservationDTO;
import com.personal.cinemabooking.entity.*;
import com.personal.cinemabooking.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private MasterDataService masterDataService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private ObjectProvider<PaymentService> paymentServiceProvider;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Showtime showtime;
    private List<Seat> seats;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUserName("testuser");
        user.setEmail("test@example.com");

        Movie movie = new Movie();
        movie.setTitle("Test Movie");

        Theater theater = new Theater();
        theater.setName("Test Theater");

        showtime = new Showtime();
        showtime.setId(1L);
        showtime.setMovie(movie);
        showtime.setTheater(theater);
        showtime.setShowDate(LocalDate.now().plusDays(1));
        showtime.setShowTime(LocalTime.of(18, 0));
        showtime.setPrice(10.0);
        showtime.setAvailableSeats(100);

        Seat seat1 = new Seat();
        seat1.setId(1L);
        seat1.setSeatNumber("A1");
        seat1.setShowtime(showtime);
        seat1.setIsReserved(false);

        Seat seat2 = new Seat();
        seat2.setId(2L);
        seat2.setSeatNumber("A2");
        seat2.setShowtime(showtime);
        seat2.setIsReserved(false);

        seats = Arrays.asList(seat1, seat2);

        reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUser(user);
        reservation.setShowtime(showtime);
        reservation.setSeats(seats);
        reservation.setReservationTime(LocalDateTime.now());
        reservation.setStatusId(1);
        reservation.setTotalPrice(20.0);
    }

    @Test
    void testGetReservationsByUser_Success() {
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(user));
        when(reservationRepository.findByUser(user)).thenReturn(Arrays.asList(reservation));
        
        // Mock MasterDataService for status value
        MasterDataDTO statusDto = new MasterDataDTO();
        statusDto.setValue("CONFIRMED");
        when(masterDataService.getMasterDataByComponentTypeNameAndMasterDataId(anyString(), anyInt()))
                .thenReturn(statusDto);

        List<ReservationDTO> result = reservationService.getReservationsByUser("testuser");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        verify(reservationRepository, times(1)).findByUser(user);
    }

    @Test
    void testCreateReservation_Success() {
        when(userRepository.findByUserNameWithLock("testuser")).thenReturn(Optional.of(user));
        when(reservationRepository.findByUserAndPaidAndStatusId(user, false, 1)).thenReturn(Arrays.asList());
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(seatRepository.findByIdInAndShowtimeIdWithLock(anyList(), eq(1L))).thenReturn(seats);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // Mock MasterDataService for status value
        MasterDataDTO statusDto = new MasterDataDTO();
        statusDto.setValue("CONFIRMED");
        when(masterDataService.getMasterDataByComponentTypeNameAndMasterDataId(anyString(), anyInt()))
                .thenReturn(statusDto);

        ReservationDTO result = reservationService.createReservation("testuser", 1L, Arrays.asList(1L, 2L));

        assertNotNull(result);
        assertEquals(20.0, result.getTotalPrice());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        verify(seatRepository, times(1)).saveAll(anyList());
        verify(showtimeRepository, times(1)).save(any(Showtime.class));
    }

    @Test
    void testCancelReservation_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // Mock MasterDataService for status value
        MasterDataDTO statusDto = new MasterDataDTO();
        statusDto.setValue("CANCELED");
        when(masterDataService.getMasterDataByComponentTypeNameAndMasterDataId(anyString(), eq(3)))
                .thenReturn(statusDto);

        ReservationDTO result = reservationService.cancelReservation(1L, "testuser");

        assertNotNull(result);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        verify(seatRepository, times(1)).saveAll(anyList());
        verify(showtimeRepository, times(1)).save(any(Showtime.class));
    }
}
