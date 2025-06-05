package com.project.Fashion.controller;

import com.project.Fashion.dto.AuthResponseDto;
import com.project.Fashion.dto.UserDto;
import com.project.Fashion.dto.UserSignInDto;
import com.project.Fashion.dto.UserSignUpDto;
import com.project.Fashion.dto.UserProfileUpdateDto;
import com.project.Fashion.service.UserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@Tag(name = "User Management", description = "APIs for user registration, login, and profile management.")
public class UserController {
    private final UserService userService;

    @Operation(summary = "Register a new user", description = "Allows new users (typically Buyers or Sellers) to register. Default role is BUYER if not specified or if an invalid role for public registration is provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., email format, password length)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"firstName\":\"First name cannot be blank\"}"))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"message\":\"Email already exists: john.doe@example.com\"}")))
    })
    @PostMapping("/register")
    public ResponseEntity<UserDto> signUp(@Valid @RequestBody UserSignUpDto dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(dto));
    }

    @Operation(summary = "Log in an existing user", description = "Authenticates a user and returns a JWT token along with user details upon successful login.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"message\":\"Invalid email or password\"}"))),
            @ApiResponse(responseCode = "429", description = "Too many login attempts (Rate limit exceeded)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"message\":\"Rate limit exceeded. Please try again later.\"}")))
    })
    @PostMapping("/login")
    @RateLimiter(name = "defaultApiService")
    public ResponseEntity<AuthResponseDto> login(@RequestBody UserSignInDto dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    @Operation(summary = "Create a new user (Admin only)", description = "Allows an administrator to create a new user with a specified role (BUYER, SELLER, or ADMIN).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully by admin",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or role not specified/invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"message\":\"Role must be specified...\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Admin token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Admin)"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping("/admin/create-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUserByAdmin(@Valid @RequestBody UserSignUpDto dto) {
        UserDto createdUser = userService.createUserByAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @Operation(summary = "Get authenticated user's profile", description = "Retrieves the profile information of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user profile",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing, invalid, or user not found)")
    })
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
            userEmail = principal.toString();
        }

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto userDto = userService.findUserDtoByEmail(userEmail);
        return ResponseEntity.ok(userDto);
    }

    @Operation(summary = "Update authenticated user's profile", description = "Allows the currently authenticated user to update their own profile information (first name, last name, email).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., email format)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"email\":\"Invalid email format\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "409", description = "Email already taken by another user")
    })
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

        UserDto currentUserDto = userService.findUserDtoByEmail(userEmail);
        if (currentUserDto == null || currentUserDto.getId() == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        UserDto updatedUser = userService.updateSelfProfile(currentUserDto.getId(), profileUpdateDto);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "Get all users (Admin only)", description = "Retrieves a list of all registered users. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Admin token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Admin)")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Get a specific user by ID (Admin or Owner only)", description = "Retrieves profile information for a specific user by their ID. Requires ADMIN privileges or the authenticated user must be the owner of the profile.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user profile",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Admin or not the owner)"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @Operation(summary = "Update a user by ID (Admin or Owner only)", description = "Updates profile information for a specific user by their ID. Requires ADMIN privileges or the authenticated user must be the owner. Password and role can be updated by Admin.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Admin or not the owner, or trying to change role without Admin privilege)"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    @RateLimiter(name = "defaultApiService")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id, @Valid @RequestBody UserSignUpDto updatedDto) {
        // Note: UserSignUpDto is used here, which includes password and role.
        // Service layer handles logic for who can update what (e.g., only admin can change role).
        return ResponseEntity.ok(userService.updateUser(id, updatedDto));
    }

    @Operation(summary = "Partially update a user by ID (Admin or Owner only)", description = "Partially updates profile information for a specific user by their ID. Requires ADMIN privileges or the authenticated user must be the owner. Only Admin can patch the 'role'.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User partially updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data or attempting to patch restricted field"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Admin or not the owner, or trying to patch role without Admin privilege)"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Email already exists if patched")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isOwner(authentication, #id)")
    @RateLimiter(name = "defaultApiService")
    public ResponseEntity<UserDto> patchUser(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        // Service layer handles logic for which fields can be patched and by whom.
        return ResponseEntity.ok(userService.patchUser(id, updates));
    }

    @Operation(summary = "Delete a user by ID (Admin only)", description = "Deletes a user account by their ID. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Admin token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an Admin)"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Logout endpoint - The actual logout logic (token blocklisting) is in CustomLogoutHandler.
    // This endpoint just needs to be reachable by authenticated users.
    @Operation(summary = "Log out the current user", description = "Logs out the currently authenticated user by blocklisting their JWT token.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful (token blocklisted)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (No active session or token to logout)")
    })
    @PostMapping("/logout")
    // @PreAuthorize("isAuthenticated()") // This is handled by SecurityConfig for the logout endpoint
    public ResponseEntity<Map<String, String>> logout() {
        // The actual token blocklisting is handled by CustomLogoutHandler.
        // This controller method can simply return a success message.
        // SecurityContextHolder is cleared by Spring Security's logout processing.
        return ResponseEntity.ok(Map.of("message", "Logout successful. Token has been blocklisted."));
    }
}
