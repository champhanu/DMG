package Ecommerce.Management.controller.catalog;

import Ecommerce.Management.dto.catalog.ProductRequest;
import Ecommerce.Management.dto.catalog.ProductResponse;
import Ecommerce.Management.service.catalog.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public Page<ProductResponse> listProducts(
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "false") boolean includeInactive,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return productService.listProducts(categoryId, search, includeInactive, pageable);
	}

	@GetMapping("/{id}")
	public ProductResponse getProduct(@PathVariable Long id) {
		return productService.getProduct(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
		return productService.createProduct(request);
	}

	@PutMapping("/{id}")
	public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return productService.updateProduct(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateProduct(@PathVariable Long id) {
		productService.deactivateProduct(id);
	}

}
