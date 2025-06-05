package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Data Transfer Object for submitting a contact message.")
public class ContactMessageRequestDto {

    @Schema(description = "Name of the person sending the message.", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Sender name cannot be blank.")
    @Size(min = 2, max = 100, message = "Sender name must be between 2 and 100 characters.")
    private String senderName;

    @Schema(description = "Email address of the sender.", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Sender email cannot be blank.")
    @Email(message = "Invalid email format.")
    @Size(max = 100, message = "Email cannot exceed 100 characters.")
    private String senderEmail;

    @Schema(description = "Subject of the contact message.", example = "Inquiry about order #123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Subject cannot be blank.")
    @Size(min = 5, max = 200, message = "Subject must be between 5 and 200 characters.")
    private String subject;

    @Schema(description = "The content of the message.", example = "I would like to know the status of my recent order...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Message content cannot be blank.")
    @Size(min = 10, max = 5000, message = "Message content must be between 10 and 5000 characters.")
    private String message;
}
