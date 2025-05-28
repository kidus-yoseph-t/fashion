package com.project.Fashion.controller;

import com.project.Fashion.dto.UserDto;
import com.project.Fashion.dto.UserSignInDto;
import com.project.Fashion.dto.UserSignUpDto;
import com.project.Fashion.service.UserService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;


    //create
    @PostMapping("/register")
    public ResponseEntity<UserDto> signUp(@RequestBody UserSignUpDto dto){
        return ResponseEntity.ok(userService.register(dto));
    }

    // login
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody UserSignInDto dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    // retrieve

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    // update
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id, @RequestBody UserSignUpDto updatedDto) {
        return ResponseEntity.ok(userService.updateUser(id, updatedDto));
    }

    // patch
    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> patchUser(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(userService.patchUser(id, updates));
    }

    // delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
