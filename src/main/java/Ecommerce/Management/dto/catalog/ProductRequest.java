package Ecommerce.Management.dto.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Z0-9_-]+$", message = "must be uppercase letters, numbers, underscores, or hyphens") String sku,
		@Size(max = 2000) String description,
		@NotNull @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal price,
		@NotNull Long categoryId
) {
}
