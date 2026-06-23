package Ecommerce.Management.service.catalog;

import Ecommerce.Management.domain.catalog.Category;
import Ecommerce.Management.domain.catalog.Product;
import Ecommerce.Management.dto.catalog.ProductRequest;
import Ecommerce.Management.dto.catalog.ProductResponse;
import Ecommerce.Management.exception.DuplicateResourceException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.catalog.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryService categoryService;

	public ProductService(ProductRepository productRepository, CategoryService categoryService) {
		this.productRepository = productRepository;
		this.categoryService = categoryService;
	}

	@Transactional(readOnly = true)
	public Page<ProductResponse> listProducts(Long categoryId, String search, boolean includeInactive, Pageable pageable) {
		String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
		return productRepository.search(categoryId, normalizedSearch, includeInactive, pageable)
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public ProductResponse getProduct(Long id) {
		return toResponse(findProduct(id));
	}

	public ProductResponse createProduct(ProductRequest request) {
		if (productRepository.existsBySku(request.sku())) {
			throw new DuplicateResourceException("Product SKU already exists: " + request.sku());
		}
		Category category = categoryService.findActiveCategory(request.categoryId());
		Product product = new Product();
		applyRequest(product, request, category);
		return toResponse(productRepository.save(product));
	}

	public ProductResponse updateProduct(Long id, ProductRequest request) {
		Product product = findProduct(id);
		if (productRepository.existsBySkuAndIdNot(request.sku(), id)) {
			throw new DuplicateResourceException("Product SKU already exists: " + request.sku());
		}
		Category category = categoryService.findActiveCategory(request.categoryId());
		applyRequest(product, request, category);
		return toResponse(product);
	}

	public void deactivateProduct(Long id) {
		Product product = findProduct(id);
		if (!product.isActive()) {
			return;
		}
		product.setActive(false);
	}

	public Product findActiveProduct(Long id) {
		Product product = findProduct(id);
		if (!product.isActive()) {
			throw new ResourceNotFoundException("Product not found: " + id);
		}
		return product;
	}

	private void applyRequest(Product product, ProductRequest request, Category category) {
		product.setName(request.name());
		product.setSku(request.sku());
		product.setDescription(request.description());
		product.setPrice(request.price());
		product.setCategory(category);
	}

	private Product findProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
	}

	private ProductResponse toResponse(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getSku(),
				product.getDescription(),
				product.getPrice(),
				product.getCategory().getId(),
				product.getCategory().getName(),
				product.isActive(),
				product.getCreatedAt(),
				product.getUpdatedAt());
	}

}
