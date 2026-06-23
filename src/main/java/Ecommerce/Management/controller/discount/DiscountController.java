package Ecommerce.Management.controller.discount;

import Ecommerce.Management.dto.discount.DiscountRequest;
import Ecommerce.Management.dto.discount.DiscountResponse;
import Ecommerce.Management.service.discount.DiscountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

	private final DiscountService discountService;

	public DiscountController(DiscountService discountService) {
		this.discountService = discountService;
	}

	@GetMapping
	public List<DiscountResponse> listActive() {
		return discountService.listActive();
	}

	@GetMapping("/{id}")
	public DiscountResponse get(@PathVariable Long id) {
		return discountService.get(id);
	}

	@PostMapping
	public DiscountResponse create(@Valid @RequestBody DiscountRequest request) {
		return discountService.create(request);
	}

	@PutMapping("/{id}")
	public DiscountResponse update(@PathVariable Long id, @Valid @RequestBody DiscountRequest request) {
		return discountService.update(id, request);
	}

}
