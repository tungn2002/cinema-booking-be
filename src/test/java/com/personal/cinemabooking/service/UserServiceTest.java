package com.personal.cinemabooking.service;

import com.personal.cinemabooking.dto.UserAdminDTO;
import com.personal.cinemabooking.dto.UserDTO;
import com.personal.cinemabooking.entity.Role;
import com.personal.cinemabooking.entity.User;
import com.personal.cinemabooking.repo.RoleRepository;
import com.personal.cinemabooking.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1);
        userRole.setName("ROLE_USER");

        adminRole = new Role();
        adminRole.setId(2);
        adminRole.setName("ROLE_ADMIN");

        user = new User();
        user.setId(1L);
        user.setUserName("testuser");
        user.setEmail("test@example.com");
        user.setRole(userRole);
        user.setReservations(new ArrayList<>());
        user.setReviews(new ArrayList<>());
    }

    @Test
    void testGetUserById_AsSelf() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(user));

        UserAdminDTO result = userService.getUserById(1L, "testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUserName());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testUpdateUser_Success() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserName("updateduser");
        userDTO.setEmail("updated@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserAdminDTO result = userService.updateUser(1L, userDTO, "testuser");

        assertNotNull(result);
        assertEquals("updateduser", user.getUserName());
        assertEquals("updated@example.com", user.getEmail());
    }

    @Test
    void testUpdateUserRole_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(2)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserAdminDTO result = userService.updateUserRole(1L, 2);

        assertNotNull(result);
        assertEquals("ROLE_ADMIN", user.getRole().getName());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testSearchUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Arrays.asList(user));
        when(userRepository.findByUserNameContainingIgnoreCaseOrEmailContainingIgnoreCase(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(userPage);

        Page<UserAdminDTO> result = userService.searchUsers("test", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUserName());
    }
}
