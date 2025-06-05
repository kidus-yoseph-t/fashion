package com.project.Fashion.controller;

import com.project.Fashion.model.ContactMessage;
import com.project.Fashion.service.ContactMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@Tag(name = "Contact Us", description = "API for submitting contact messages from users to administrators.")
public class ContactMessageController {

    @Autowired
    private ContactMessageService contactMessageService;

    @Operation(summary = "Submit a contact message",
            description = "Allows any user (public access) to submit a contact message (e.g., inquiry, feedback). The message is then stored for admin review.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message sent successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", example = "Message sent successfully!"))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (if ContactMessage DTO had validation annotations and @Valid was used)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"senderName\":\"Name cannot be blank\"}"))),
            @ApiResponse(responseCode = "500", description = "Failed to send message due to an internal server error",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", example = "Failed to send message.")))
    })
    @PostMapping
    public ResponseEntity<String> submitContactMessage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody( // Used fully qualified name
                    description = "Contact message details. Ensure all required fields (senderName, senderEmail, subject, message) are provided.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ContactMessage.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody ContactMessage message) { // Used fully qualified name for Spring's RequestBody for clarity
        try {
            contactMessageService.saveMessage(message);
            return ResponseEntity.status(HttpStatus.CREATED).body("Message sent successfully!");
        } catch (Exception e) {
            System.err.println("Error saving contact message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send message.");
        }
    }
}
