package com.project.Fashion.service;

import com.project.Fashion.config.mappers.UserMapper;
import com.project.Fashion.dto.AuthResponseDto;
import com.project.Fashion.dto.UserDto;
import com.project.Fashion.dto.UserSignInDto;
import com.project.Fashion.dto.UserSignUpDto;
import com.project.Fashion.exception.exceptions.EmailAlreadyExistsException;
import com.project.Fashion.exception.exceptions.InvalidCredentialsException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.security.JwtUtil; // Import JwtUtil
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager; // Import AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Import UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails; // Import UserDetails
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager; // Inject AuthenticationManager
    private final JwtUtil jwtUtil; // Inject JwtUtil

    @Transactional
    public UserDto register(UserSignUpDto dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
        }
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // Ensure role is not null and set default if necessary, or validate
        if (dto.getRole() == null || dto.getRole().trim().isEmpty()) {
            throw new IllegalArgumentException("Role must be provided for user registration.");
        }
        user.setRole(dto.getRole().toUpperCase()); // Store roles in uppercase consistently
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    public AuthResponseDto login(UserSignInDto dto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );

            // If authentication is successful, Spring Security context will hold the principal
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found after authentication")); // Should not happen

            final String token = jwtUtil.generateToken(userDetails);
            return new AuthResponseDto(userMapper.toDto(user), token);

        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    public UserDto getUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUser(String id, UserSignUpDto dto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        existing.setFirstname(dto.getFirstname());
        existing.setLastname(dto.getLastname());

        // Handle email change carefully, check for duplicates if email is updated
        if (!existing.getEmail().equals(dto.getEmail())) {
            if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
            }
            existing.setEmail(dto.getEmail());
        }

        if (dto.getRole() != null && !dto.getRole().trim().isEmpty()) {
            existing.setRole(dto.getRole().toUpperCase());
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userRepository.save(existing);

        return userMapper.toDto(existing);
    }

    @Transactional
    public UserDto patchUser(String id, Map<String, Object> updates) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        updates.forEach((key, value) -> {
            switch (key) {
                case "firstname":
                    user.setFirstname((String) value);
                    break;
                case "lastname":
                    user.setLastname((String) value);
                    break;
                case "email":
                    String newEmail = (String) value;
                    if (!user.getEmail().equals(newEmail)) {
                        if (userRepository.findByEmail(newEmail).isPresent()) {
                            throw new EmailAlreadyExistsException("Email already exists: " + newEmail);
                        }
                        user.setEmail(newEmail);
                    }
                    break;
                case "role":
                    String newRole = (String) value;
                    if (newRole != null && !newRole.trim().isEmpty()) {
                        user.setRole(newRole.toUpperCase());
                    }
                    break;
                case "password":
                    String newPassword = (String) value;
                    if (newPassword != null && !newPassword.isEmpty()) {
                        user.setPassword(passwordEncoder.encode(newPassword));
                    }
                    break;
                default:
                    // Log or throw an exception for unknown fields
                    break;
            }
        });

        userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}