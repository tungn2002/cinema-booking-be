package com.personal.cinemabooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// main app entry point - starts the spring boot app
@SpringBootApplication
public class CinemaBookingApiApplication {
	// main method - this is where everything begins
	public static void main(String[] args) {
		// fire up the app!
		SpringApplication.run(CinemaBookingApiApplication.class, args);
	}
}