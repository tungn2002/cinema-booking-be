package com.personal.cinemabooking.config;

import com.personal.cinemabooking.entity.Role;
import com.personal.cinemabooking.entity.User;
import com.personal.cinemabooking.repo.RoleRepository;
import com.personal.cinemabooking.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${spring.web.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins; // frontend origins

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        Role userRole = roleRepository.findByName("ROLE_USER");

        // check DB
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setUserName(name);
                    newUser.setPassword(UUID.randomUUID().toString());
                    newUser.setRole(userRole);
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(user);

        String redirectUrl = allowedOrigins.split(",")[0];
        getRedirectStrategy().sendRedirect(
                request,
                response,
                redirectUrl + "?token=" + token
        );
    }
}