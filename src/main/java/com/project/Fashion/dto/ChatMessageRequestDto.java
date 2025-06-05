package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Data Transfer Object for sending a chat message via HTTP.")
public class ChatMessageRequestDto {

    @Schema(description = "ID of the conversation this message belongs to.", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Conversation ID cannot be null.")
    private Long conversationId;

    // senderId will be taken from authenticated user on backend for security
    // and is not expected in the request body for this DTO.

    @Schema(description = "Content of the chat message.", example = "Hello, I have a question about my order.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Message content cannot be blank.")
    @Size(min = 1, max = 2000, message = "Message content must be between 1 and 2000 characters.")
    private String content;
}
