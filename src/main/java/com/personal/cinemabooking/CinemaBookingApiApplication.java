package com.personal.cinemabooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.retry.annotation.EnableRetry;

// main app entry point - starts the spring boot app
@SpringBootApplication
@EnableScheduling // Enable scheduled tasks for auto-canceling expired reservations
@EnableAsync
@EnableRetry
public class CinemaBookingApiApplication {
	// main method - this is where everything begins
	public static void main(String[] args) {
		// fire up the app!
		SpringApplication.run(CinemaBookingApiApplication.class, args);
	}
}