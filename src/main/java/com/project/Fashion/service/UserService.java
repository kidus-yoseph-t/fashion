package com.project.Fashion.service;

import com.project.Fashion.config.mappers.UserMapper;
import com.project.Fashion.dto.AuthResponseDto;
import com.project.Fashion.dto.UserDto;
import com.project.Fashion.dto.UserSignInDto;
import com.project.Fashion.dto.UserSignUpDto;
import com.project.Fashion.dto.UserProfileUpdateDto; // For self-profile updates
import com.project.Fashion.exception.exceptions.EmailAlreadyExistsException;
import com.project.Fashion.exception.exceptions.InvalidCredentialsException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private static final Set<String> ALLOWED_PUBLIC_REGISTRATION_ROLES = Set.of("BUYER", "SELLER");
    private static final String DEFAULT_REGISTRATION_ROLE = "BUYER";
    private static final Set<String> VALID_ROLES = Set.of("BUYER", "SELLER", "ADMIN");

    @Transactional
    public UserDto register(UserSignUpDto dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
        }
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        String requestedRole = dto.getRole();

        if (StringUtils.hasText(requestedRole)) {
            String upperCaseRole = requestedRole.toUpperCase();
            if (ALLOWED_PUBLIC_REGISTRATION_ROLES.contains(upperCaseRole)) {
                user.setRole(upperCaseRole);
            } else {
                logger.warn("Attempt to register with an unauthorized role '{}' via public endpoint for email {}. Defaulting to {}.",
                        requestedRole, dto.getEmail(), DEFAULT_REGISTRATION_ROLE);
                user.setRole(DEFAULT_REGISTRATION_ROLE);
            }
        } else {
            logger.info("No role provided for public registration for email {}. Defaulting to {}.", dto.getEmail(), DEFAULT_REGISTRATION_ROLE);
            user.setRole(DEFAULT_REGISTRATION_ROLE);
        }

        userRepository.save(user);
        logger.info("User registered successfully with email {} and role {}", user.getEmail(), user.getRole());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto createUserByAdmin(UserSignUpDto dto) {
        logger.info("Admin attempting to create user with email: {}", dto.getEmail());
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
        }

        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        String requestedRole = dto.getRole();
        if (StringUtils.hasText(requestedRole)) {
            String upperCaseRole = requestedRole.toUpperCase();
            if (VALID_ROLES.contains(upperCaseRole)) {
                user.setRole(upperCaseRole);
            } else {
                logger.error("Admin attempted to create user with invalid role: {}. Valid roles are: {}", requestedRole, VALID_ROLES);
                throw new IllegalArgumentException("Invalid role specified: " + requestedRole + ". Valid roles are: " + VALID_ROLES);
            }
        } else {
            logger.error("Admin user creation attempt failed: Role must be specified by admin for email {}", dto.getEmail());
            throw new IllegalArgumentException("Role must be specified when an admin creates a user.");
        }

        userRepository.save(user);
        logger.info("User {} created successfully by admin with role {}", user.getEmail(), user.getRole());
        return userMapper.toDto(user);
    }

    public AuthResponseDto login(UserSignInDto dto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found after authentication for email: " + userDetails.getUsername()));

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

    private boolean isAuthenticatedUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    @Transactional
    public UserDto updateSelfProfile(String authenticatedUserId, UserProfileUpdateDto dto) {
        User currentUser = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with ID: " + authenticatedUserId));

        boolean changed = false;

        if (StringUtils.hasText(dto.getFirstName()) && !dto.getFirstName().equals(currentUser.getFirstName())) {
            currentUser.setFirstName(dto.getFirstName());
            changed = true;
        }
        if (StringUtils.hasText(dto.getLastName()) && !dto.getLastName().equals(currentUser.getLastName())) {
            currentUser.setLastName(dto.getLastName());
            changed = true;
        }
        if (StringUtils.hasText(dto.getEmail()) && !dto.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            if (userRepository.findByEmail(dto.getEmail()).filter(u -> !u.getId().equals(currentUser.getId())).isPresent()) {
                throw new EmailAlreadyExistsException("Email " + dto.getEmail() + " is already taken.");
            }
            currentUser.setEmail(dto.getEmail());
            changed = true;
        }

        if (changed) {
            userRepository.save(currentUser);
            logger.info("User profile updated for user ID: {}", authenticatedUserId);
        } else {
            logger.info("No changes detected for user profile update for user ID: {}", authenticatedUserId);
        }
        return userMapper.toDto(currentUser);
    }

    @Transactional
    public UserDto updateUser(String id, UserSignUpDto updatedDto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        existingUser.setFirstName(updatedDto.getFirstName());
        existingUser.setLastName(updatedDto.getLastName());

        if (StringUtils.hasText(updatedDto.getEmail()) && !existingUser.getEmail().equals(updatedDto.getEmail())) {
            if (userRepository.findByEmail(updatedDto.getEmail()).isPresent()) {
                throw new EmailAlreadyExistsException("Email already exists: " + updatedDto.getEmail());
            }
            existingUser.setEmail(updatedDto.getEmail());
        }

        if (StringUtils.hasText(updatedDto.getRole())) {
            String newRole = updatedDto.getRole().toUpperCase();
            if (!existingUser.getRole().equals(newRole)) {
                if (isAuthenticatedUserAdmin()) {
                    if (VALID_ROLES.contains(newRole)) {
                        logger.info("Admin {} is changing role of user {} to {}", authenticatedUsername, id, newRole);
                        existingUser.setRole(newRole);
                    } else {
                        logger.warn("Admin {} attempted to change role of user {} to invalid role {}. Change denied.", authenticatedUsername, id, newRole);
                        throw new IllegalArgumentException("Cannot update to invalid role: " + newRole + ". Valid roles are: " + VALID_ROLES);
                    }
                } else {
                    logger.warn("User {} (not an Admin) attempted to change role for user {} to {}. Role change denied.",
                            authenticatedUsername, id, newRole);
                }
            }
        }

        if (StringUtils.hasText(updatedDto.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(updatedDto.getPassword()));
        }
        userRepository.save(existingUser);

        return userMapper.toDto(existingUser);
    }

    @Transactional
    public UserDto patchUser(String id, Map<String, Object> updates) {
        User userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = (authentication != null) ? authentication.getName() : "SYSTEM";

        updates.forEach((key, value) -> {
            switch (key) {
                case "firstName":
                    if (value != null) userToUpdate.setFirstName((String) value);
                    break;
                case "lastName":
                    if (value != null) userToUpdate.setLastName((String) value);
                    break;
                case "email":
                    String newEmail = (String) value;
                    if (StringUtils.hasText(newEmail) && !userToUpdate.getEmail().equals(newEmail)) {
                        userRepository.findByEmail(newEmail).ifPresent(existingUserWithNewEmail -> {
                            if (!existingUserWithNewEmail.getId().equals(userToUpdate.getId())) {
                                throw new EmailAlreadyExistsException("Email " + newEmail + " is already taken.");
                            }
                        });
                        userToUpdate.setEmail(newEmail);
                    }
                    break;
                case "role":
                    String newRoleFromPatch = (String) value;
                    if (StringUtils.hasText(newRoleFromPatch)) {
                        String upperCaseNewRole = newRoleFromPatch.toUpperCase();
                        if (!userToUpdate.getRole().equals(upperCaseNewRole)) {
                            if (isAuthenticatedUserAdmin()) {
                                if (VALID_ROLES.contains(upperCaseNewRole)) {
                                    logger.info("Admin {} is patching role of user {} to {}", authenticatedUsername, id, upperCaseNewRole);
                                    userToUpdate.setRole(upperCaseNewRole);
                                } else {
                                    logger.warn("Admin {} attempted to patch role of user {} to invalid role {}. Patch denied.", authenticatedUsername, id, upperCaseNewRole);
                                    throw new IllegalArgumentException("Cannot patch to invalid role: " + upperCaseNewRole + ". Valid roles are: " + VALID_ROLES);
                                }
                            } else {
                                logger.warn("User {} (not an Admin) attempted to patch role for user {} to {}. Role patch denied.",
                                        authenticatedUsername, id, upperCaseNewRole);
                            }
                        }
                    }
                    break;
                case "password":
                    String newPassword = (String) value;
                    if (StringUtils.hasText(newPassword)) {
                        userToUpdate.setPassword(passwordEncoder.encode(newPassword));
                    }
                    break;
                default:
                    logger.warn("Attempted to patch unknown or restricted field '{}' for user id: {}", key, id);
                    break;
            }
        });

        userRepository.save(userToUpdate);
        return userMapper.toDto(userToUpdate);
    }

    @Transactional
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public UserDto findUserDtoByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}