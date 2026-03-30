package com.personal.cinemabooking.dto;

import lombok.*;

// response after successful registration
@Data
@AllArgsConstructor
public class RegisterResponse {
    private String username;  // echo back the username

    // TODO: maybe add userId or token later?
}