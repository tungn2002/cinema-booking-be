package com.personal.cinemabooking.controller;

import com.personal.cinemabooking.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieControllerPosterTest {

    @Mock
    private MovieService movieService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private MovieController movieController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(movieController).build();

        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Success message");
    }

}
