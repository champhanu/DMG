package Ecommerce.Management.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(max = 32) @Pattern(regexp = "^[A-Z0-9_-]+$") String code,
		@Size(max = 255) String location
) {
}
