package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for user registration")
public class UserSignUpDto {

    @Schema(description = "User's first name.", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "First name cannot be blank")
    private String firstName;

    @Schema(description = "User's last name.", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Last name cannot be blank")
    private String lastName;

    @Schema(description = "User's email address.", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User's password. Must be at least 8 characters long.", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @Schema(description = "Role for the user. Can be 'BUYER' or 'SELLER'. Defaults to 'BUYER' if not specified.", example = "BUYER")
    private String role;
}