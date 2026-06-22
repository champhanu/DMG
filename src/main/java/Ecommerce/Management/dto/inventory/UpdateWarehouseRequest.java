package Ecommerce.Management.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWarehouseRequest(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 255) String location
) {
}
