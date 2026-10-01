package com.personal.cinemabooking.controllers;

import lombok.RequiredArgsConstructor;

import com.personal.cinemabooking.dto.ApiResponse;
import com.personal.cinemabooking.entities.ComponentType;
import com.personal.cinemabooking.entities.MasterData;
import com.personal.cinemabooking.repositories.ComponentTypeRepository;
import com.personal.cinemabooking.repositories.MasterDataRepository;
import com.personal.cinemabooking.repositories.ReservationRepository;
import com.personal.cinemabooking.repositories.SeatRepository;
import com.personal.cinemabooking.repositories.ShowtimeRepository;
import com.personal.cinemabooking.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// for troubleshooting database issues
// admin-only endpoints to check db state when things go wrong
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/diagnostics") // base path for diagnostic endpoints
@Slf4j // logging
public class DiagnosticController {

    private final UserRepository userRepository; // for user counts

    private final ReservationRepository reservationRepository; // for reservation counts

    private final SeatRepository seatRepository; // for seat counts

    private final ShowtimeRepository showtimeRepository; // for showtime counts

    private final ComponentTypeRepository componentTypeRepository; // for component types

    private final MasterDataRepository masterDataRepository; // for master data

    // get database diagnostics including entity counts and key data
    // useful for checking if db is properly initialized
    @GetMapping("/database")
    @PreAuthorize("hasRole('ROLE_ADMIN')") // admin only
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseDiagnostics() {
        log.info("Fetching database diagnostics");
        Map<String, Object> diagnostics = new HashMap<>();

        // Count entities - basic health check
        diagnostics.put("userCount", userRepository.count());
        diagnostics.put("reservationCount", reservationRepository.count());
        diagnostics.put("seatCount", seatRepository.count()); // should be a lot
        diagnostics.put("showtimeCount", showtimeRepository.count());

        // Check if admin user exists - critical check
        boolean adminExists = userRepository.findByUserName("admin").isPresent();
        diagnostics.put("adminExists", adminExists);

        // Check component types and master data
        // this is important for reservation status codes
        Optional<ComponentType> reservationStatusType = componentTypeRepository.findByName("RESERVATION_STATUS");
        diagnostics.put("reservationStatusTypeExists", reservationStatusType.isPresent());

        if (reservationStatusType.isPresent()) {
            List<MasterData> statusValues = masterDataRepository.findByComponentType(reservationStatusType.get());
            diagnostics.put("reservationStatusCount", statusValues.size());

            // Log the status values for debugging
            // helps identify missing statuses
            Map<Integer, String> statusMap = new HashMap<>();
            statusValues.forEach(status -> {
                statusMap.put(status.getMasterDataId(), status.getValue());
                log.info("Status ID: {}, Value: {}", status.getMasterDataId(), status.getValue());
            });
            diagnostics.put("reservationStatuses", statusMap);
        }

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Database diagnostics retrieved successfully",
                diagnostics
        ));
    }
} // end of DiagnosticController
