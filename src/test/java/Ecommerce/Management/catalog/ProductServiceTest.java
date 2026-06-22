package Ecommerce.Management.catalog;

import Ecommerce.Management.domain.catalog.Category;
import Ecommerce.Management.domain.catalog.Product;
import Ecommerce.Management.exception.DuplicateResourceException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.catalog.CategoryRepository;
import Ecommerce.Management.repository.catalog.ProductRepository;
import Ecommerce.Management.service.catalog.CategoryService;
import Ecommerce.Management.service.catalog.ProductService;
import Ecommerce.Management.dto.catalog.CategoryRequest;
import Ecommerce.Management.dto.catalog.ProductRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryService categoryService;

	@InjectMocks
	private ProductService productService;

	@Test
	void createProductThrowsWhenSkuExists() {
		ProductRequest request = new ProductRequest(
				"Test Product", "SKU-001", "desc", new BigDecimal("10.00"), 1L);

		when(productRepository.existsBySku("SKU-001")).thenReturn(true);

		assertThatThrownBy(() -> productService.createProduct(request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessageContaining("SKU-001");
	}

	@Test
	void getProductThrowsWhenMissing() {
		when(productRepository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getProduct(42L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("42");
	}

	@Test
	void createProductPersistsWhenValid() {
		ProductRequest request = new ProductRequest(
				"Test Product", "SKU-001", "desc", new BigDecimal("10.00"), 1L);
		Category category = new Category();
		category.setName("Electronics");
		category.setSlug("electronics");
		category.setActive(true);

		when(productRepository.existsBySku("SKU-001")).thenReturn(false);
		when(categoryService.findActiveCategory(1L)).thenReturn(category);
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
			Product product = invocation.getArgument(0);
			return product;
		});

		productService.createProduct(request);

		verify(productRepository).save(any(Product.class));
	}

}
