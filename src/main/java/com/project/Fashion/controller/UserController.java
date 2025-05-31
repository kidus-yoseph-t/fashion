package com.project.Fashion.controller;

import com.project.Fashion.dto.AuthResponseDto; // Import AuthResponseDto
import com.project.Fashion.dto.UserDto;
import com.project.Fashion.dto.UserSignInDto;
import com.project.Fashion.dto.UserSignUpDto;
import com.project.Fashion.service.UserService;
import lombok.AllArgsConstructor;
// import lombok.RequiredArgsConstructor; // Replaced by AllArgsConstructor
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/users")
@AllArgsConstructor // Use AllArgsConstructor if all fields are final and part of constructor
public class UserController {
    private final UserService userService;


    //create - Public
    @PostMapping("/register")
    public ResponseEntity<UserDto> signUp(@RequestBody UserSignUpDto dto){
        return ResponseEntity.ok(userService.register(dto));
    }

    // login - Public
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody UserSignInDto dto) { // Return AuthResponseDto
        return ResponseEntity.ok(userService.login(dto));
    }

    // retrieve all users - Example: Admin only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // Example, adjust role as needed
    public ResponseEntity<List<UserDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // retrieve specific user - Example: Admin or the user themselves
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)") // Example of custom security
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