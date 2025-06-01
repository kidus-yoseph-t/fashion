package com.project.Fashion.controller;

import com.project.Fashion.dto.AuthResponseDto;
import com.project.Fashion.dto.UserDto;
import com.project.Fashion.dto.UserSignInDto;
import com.project.Fashion.dto.UserSignUpDto;
import com.project.Fashion.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.security.core.context.SecurityContextHolder; // Import SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails; // Import UserDetails

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    // ... (Your existing signUp and login methods) ...

    // create - Public
    @PostMapping("/register")
    public ResponseEntity<UserDto> signUp(@RequestBody UserSignUpDto dto){
        return ResponseEntity.ok(userService.register(dto));
    }

    // login - Public
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody UserSignInDto dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    // --- NEW ENDPOINT FOR GETTING CURRENT USER'S PROFILE ---
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // Ensures only authenticated users can access their own profile
    public ResponseEntity<UserDto> getAuthenticatedUser() {
        // Retrieve the authenticated user's email (or username) from Spring Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = null;

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else if (authentication != null && authentication.getPrincipal() instanceof String) {
            userEmail = (String) authentication.getPrincipal();
        }

        if (userEmail == null) {
            // This case should ideally not happen with @PreAuthorize("isAuthenticated()")
            // but good for defensive programming.
            return ResponseEntity.status(401).build(); // Unauthorized if principal is not found
        }

        // Call a UserService method to get the user's DTO by their email
        // You'll need to add a method like findUserDtoByEmail(String email) to your UserService
        UserDto userDto = userService.findUserDtoByEmail(userEmail); // Assume this method exists
        return ResponseEntity.ok(userDto);
    }
    // --- END NEW ENDPOINT ---


    // retrieve all users - Example: Admin only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // retrieve specific user - Example: Admin or the user themselves
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    // update - Example: Admin or the user themselves
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id, @RequestBody UserSignUpDto updatedDto) {
        return ResponseEntity.ok(userService.updateUser(id, updatedDto));
    }

    // patch - Example: Admin or the user themselves
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserDto> patchUser(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(userService.patchUser(id, updates));
    }

    // delete - Example: Admin only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}