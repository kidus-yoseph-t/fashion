package com.project.Fashion.controller;

import com.project.Fashion.model.ContactMessage;
import com.project.Fashion.service.ContactMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/AdminMessages")
@Tag(name = "Admin: Contact Messages", description = "APIs for administrators to manage contact messages submitted by users.")
@SecurityRequirement(name = "bearerAuth") // All endpoints in this controller require ADMIN role
@PreAuthorize("hasRole('ADMIN')") // Class-level security, all methods require ADMIN
public class AdminMessageController {

    @Autowired
    private ContactMessageService contactMessageService;

    @Operation(summary = "Get all contact messages",
            description = "Retrieves a list of all contact messages submitted through the website. Requires ADMIN privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all contact messages",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ContactMessage.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Admin token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an ADMIN)")
    })
    @GetMapping
    public ResponseEntity<List<ContactMessage>> getAllAdminMessages() {
        List<ContactMessage> messages = contactMessageService.getAllMessages();
        return ResponseEntity.ok(messages);
    }

    @Operation(summary = "Get a specific contact message by ID",
            description = "Retrieves details for a specific contact message by its unique ID. Requires ADMIN privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved contact message details",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContactMessage.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Admin token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Contact message not found with the given ID")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContactMessage> getMessageById(
            @Parameter(description = "ID of the contact message to retrieve", required = true, example = "1")
            @PathVariable Long id) {
        return contactMessageService.getMessageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update the status of a contact message",
            description = "Updates the status (e.g., 'read', 'unread') of a specific contact message. Requires ADMIN privileges. The request body should be a JSON object like: {\"status\": \"read\"}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact message status updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContactMessage.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status provided in request body (must be 'read' or 'unread')",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"error\":\"Invalid status value\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Admin token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Contact message not found with the given ID")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ContactMessage> updateMessageStatus(
            @Parameter(description = "ID of the contact message to update status for", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "JSON object containing the new status. Example: {\"status\": \"read\"}",
                    required = true,
                    schema = @Schema(type = "object", example = "{\"status\": \"read\"}"))
            @RequestBody Map<String, String> statusUpdate) {
        String newStatus = statusUpdate.get("status");
        if (newStatus == null || (!newStatus.equalsIgnoreCase("read") && !newStatus.equalsIgnoreCase("unread"))) {
            // It's good practice to return a more informative error body for 400
            return ResponseEntity.badRequest().body(null); // Or a custom error DTO
        }

        ContactMessage updatedMessage = contactMessageService.updateMessageStatus(id, newStatus.toLowerCase()); // Store as lowercase
        if (updatedMessage != null) {
            return ResponseEntity.ok(updatedMessage);
        } else {
            return ResponseEntity.notFound().build(); // Message not found by service
        }
    }

    @Operation(summary = "Delete a contact message",
            description = "Deletes a specific contact message by its ID. Requires ADMIN privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Contact message deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Admin token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not an ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Contact message not found with the given ID (if service handles this by not erroring on delete of non-existent)")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdminMessage(
            @Parameter(description = "ID of the contact message to delete", required = true, example = "1")
            @PathVariable Long id) {
        // Assuming service method doesn't throw exception if ID not found, but simply does nothing.
        // If it throws an exception (e.g. EmptyResultDataAccessException), GlobalExceptionHandler would handle it.
        contactMessageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }
}
