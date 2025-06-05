package com.project.Fashion.controller;

import com.project.Fashion.dto.ContactMessageRequestDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/contact")
@Tag(name = "Contact Us", description = "API for submitting contact messages from users to administrators.")
public class ContactMessageController {

    private static final Logger logger = LoggerFactory.getLogger(ContactMessageController.class); // Added logger

    @Autowired
    private ContactMessageService contactMessageService;

    @Operation(summary = "Submit a contact message",
            description = "Allows any user (public access) to submit a contact message. The message is then stored for admin review.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message sent successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", example = "Message sent successfully!"))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., missing fields, invalid email format)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"senderName\":\"Sender name cannot be blank.\"}"))),
            @ApiResponse(responseCode = "500", description = "Failed to send message due to an internal server error",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string", example = "Failed to send message.")))
    })
    @PostMapping
    public ResponseEntity<String> submitContactMessage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Contact message details.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ContactMessageRequestDto.class))) // Use DTO for schema
            @Valid @org.springframework.web.bind.annotation.RequestBody ContactMessageRequestDto messageRequestDto) { // Use DTO for request
        try {
            // Manually map DTO to Entity
            ContactMessage contactMessage = new ContactMessage();
            contactMessage.setSenderName(messageRequestDto.getSenderName());
            contactMessage.setSenderEmail(messageRequestDto.getSenderEmail());
            contactMessage.setSubject(messageRequestDto.getSubject());
            contactMessage.setMessage(messageRequestDto.getMessage());
            // createdAt and status are handled by the entity's default constructor or @PrePersist

            contactMessageService.saveMessage(contactMessage);
            return ResponseEntity.status(HttpStatus.CREATED).body("Message sent successfully!");
        } catch (Exception e) {
            logger.error("Error saving contact message from DTO: {}", messageRequestDto, e); // Log with DTO details
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send message.");
        }
    }
}
