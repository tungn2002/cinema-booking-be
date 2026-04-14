package com.personal.cinemabooking.service;

import com.personal.cinemabooking.dto.ReviewDTO;
import com.personal.cinemabooking.dto.ReviewRequest;
import com.personal.cinemabooking.entity.Movie;
import com.personal.cinemabooking.entity.Review;
import com.personal.cinemabooking.entity.User;
import com.personal.cinemabooking.repo.MovieRepository;
import com.personal.cinemabooking.repo.ReviewRepository;
import com.personal.cinemabooking.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private UserBlockService userBlockService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ReviewService reviewService;

    private Review review;
    private Movie movie;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUserName("testuser");
        com.personal.cinemabooking.entity.Role role = new com.personal.cinemabooking.entity.Role();
        role.setName("ROLE_ADMIN");
        user.setRole(role);

        movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Test Movie");

        review = new Review();
        review.setId(1L);
        review.setUser(user);
        review.setMovie(movie);
        review.setRating(5);
        review.setComment("Great movie!");
        review.setStatus(Review.ReviewStatus.PENDING);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpvotes(0);
        review.setDownvotes(0);
    }

    @Test
    void testAddReview_Success() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setComment("Great!");

        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(user));
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(userBlockService.isUserBlockedByAdmin(1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewRepository.findById(any())).thenReturn(Optional.of(review));

        ReviewDTO result = reviewService.addReview("testuser", 1L, request);

        assertNotNull(result);
        assertEquals(5, result.getRating());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void testGetReviewsByMovie_Success() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(user));
        // For admin, it calls findByMovie
        when(reviewRepository.findByMovie(movie)).thenReturn(Arrays.asList(review));

        List<ReviewDTO> result = reviewService.getReviewsByMovie(1L, "testuser");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testApproveReview_Success() {
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        ReviewDTO result = reviewService.approveReview(1L, "testuser");

        assertNotNull(result);
        assertEquals(Review.ReviewStatus.APPROVED, result.getStatus());
        verify(reviewRepository, times(1)).save(review);
    }
}
