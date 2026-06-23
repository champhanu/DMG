package Ecommerce.Management.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderReturnRequest(
		@NotNull Long customerId,
		@NotBlank @Size(max = 500) String reason
) {
}
