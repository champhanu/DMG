package Ecommerce.Management.controller.checkout;

import Ecommerce.Management.dto.checkout.CheckoutRequest;
import Ecommerce.Management.dto.checkout.CheckoutResponse;
import Ecommerce.Management.service.checkout.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

	private final CheckoutService checkoutService;

	public CheckoutController(CheckoutService checkoutService) {
		this.checkoutService = checkoutService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
		return checkoutService.checkout(request);
	}

}
