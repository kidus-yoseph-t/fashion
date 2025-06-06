package com.project.Fashion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*; // For validation annotations
import lombok.Data;

@Data
@Schema(description = "Data Transfer Object for Product Reviews. Used for creating and updating reviews.")
public class ReviewDto {

    @Schema(description = "Unique identifier of the Review.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "ID of the product being reviewed. Set from path variable, not usually in request body for add.", example = "101", accessMode = Schema.AccessMode.READ_ONLY)
    private Long productId; // Typically set from path variable, not in request body for add.

    @Schema(description = "ID of the user submitting the review. Set from path variable/authenticated user, not usually in request body for add.", example = "user-uuid-abc", accessMode = Schema.AccessMode.READ_ONLY)
    private String userId;  // Typically set from path variable or authenticated user context.

    @Schema(description = "Name of the user who submitted the review.", example = "John Doe", accessMode = Schema.AccessMode.READ_ONLY)
    private String userName; // Populated by the backend

    @Schema(description = "Rating given by the user (e.g., 1 to 5).", example = "4.5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Rating cannot be null.")
    @DecimalMin(value = "0.5", message = "Rating must be at least 0.5.") // Assuming a 0.5 to 5 scale
    @DecimalMax(value = "5.0", message = "Rating must be at most 5.0.")
    private Float rating; // Changed to Float for @NotNull

    @Schema(description = "Comment text for the review.", example = "Great product, highly recommended!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Comment cannot be blank.")
    @Size(min = 10, max = 2000, message = "Comment must be between 10 and 2000 characters.")
    private String comment;

    @Schema(description = "Date when the review was submitted or last updated.", example = "2024-06-05 10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private String date; // ISO string or formatted date, populated by backend
}