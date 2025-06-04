package com.project.Fashion.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
// No @NotBlank because fields are optional for PATCH;
// if a field is present, it should meet other constraints.

@Data
public class UserProfileUpdateDto {

    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName; // Optional: user might only want to update lastName or email

    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;  // Optional

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;     // Optional

    // Password and role are intentionally omitted as they should be handled
    // by separate, more secure processes (e.g., "change password" endpoint, admin role management).
}
