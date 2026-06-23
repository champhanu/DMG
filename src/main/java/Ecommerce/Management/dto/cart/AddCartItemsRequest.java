package Ecommerce.Management.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddCartItemsRequest(
		@NotNull Long customerId,
		@NotEmpty List<@Valid CartItemLineRequest> items
) {
}
