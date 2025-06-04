package com.project.Fashion.controller;

import com.project.Fashion.dto.AuthResponseDto;
import com.project.Fashion.dto.UserDto;
import com.project.Fashion.dto.UserSignInDto;
import com.project.Fashion.dto.UserSignUpDto;
import com.project.Fashion.dto.UserProfileUpdateDto; // For self-profile updates
import com.project.Fashion.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> signUp(@Valid @RequestBody UserSignUpDto dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody UserSignInDto dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    @PostMapping("/admin/create-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUserByAdmin(@Valid @RequestBody UserSignUpDto dto) {
        UserDto createdUser = userService.createUserByAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            userEmail = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            userEmail = (String) principal;
        } else if (principal != null) {
            userEmail = principal.toString(); // Fallback, might not be email
        }


        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto userDto = userService.findUserDtoByEmail(userEmail);
        return ResponseEntity.ok(userDto);
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateMyProfile(@Valid @RequestBody UserProfileUpdateDto profileUpdateDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            userEmail = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            userEmail = (String) principal;
        } else if (principal != null) {
            userEmail = principal.toString();
        }

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDto currentUserDto = userService.findUserDtoByEmail(userEmail); // Ensure this method exists and works
        if (currentUserDto == null || currentUserDto.getId() == null) {
            // This case should ideally not be reached if user is authenticated
            // and findUserDtoByEmail is robust (throws if not found)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // Or a more specific error
        }

        UserDto updatedUser = userService.updateSelfProfile(currentUserDto.getId(), profileUpdateDto);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id, @Valid @RequestBody UserSignUpDto updatedDto) {
        return ResponseEntity.ok(userService.updateUser(id, updatedDto));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserDto> patchUser(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(userService.patchUser(id, updates));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}