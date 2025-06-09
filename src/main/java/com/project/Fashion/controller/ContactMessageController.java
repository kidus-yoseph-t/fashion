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

    private static final Logger logger = LoggerFactory.getLogger(ContactMessageController.class);

    @Autowired
    private ContactMessageService contactMessageService;

    @Operation(summary = "Submit a contact message",
            description = "Allows any user (public access) to submit a contact message. The message is then stored for admin review.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message sent successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ContactMessage.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"senderName\":\"Sender name cannot be blank.\"}"))),
            @ApiResponse(responseCode = "500", description = "Failed to send message due to an internal server error")
    })
    @PostMapping
    public ResponseEntity<ContactMessage> submitContactMessage(
            @Valid @RequestBody ContactMessageRequestDto messageRequestDto) {

        logger.info("Received contact message submission from: {}", messageRequestDto.getSenderEmail());

        // The service now handles mapping and saving.
        // This also removes the local try-catch to allow the GlobalExceptionHandler to manage errors.
        ContactMessage savedMessage = contactMessageService.saveMessageFromDto(messageRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedMessage);
    }
}