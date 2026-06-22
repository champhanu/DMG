package Ecommerce.Management.controller.catalog;

import Ecommerce.Management.dto.catalog.CategoryRequest;
import Ecommerce.Management.dto.catalog.CategoryResponse;
import Ecommerce.Management.service.catalog.CategoryService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	public List<CategoryResponse> listCategories(
			@RequestParam(required = false) Long parentId,
			@RequestParam(defaultValue = "false") boolean includeInactive) {
		return categoryService.listCategories(parentId, includeInactive);
	}

	@GetMapping("/{id}")
	public CategoryResponse getCategory(@PathVariable Long id) {
		return categoryService.getCategory(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
		return categoryService.createCategory(request);
	}

	@PutMapping("/{id}")
	public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return categoryService.updateCategory(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateCategory(@PathVariable Long id) {
		categoryService.deactivateCategory(id);
	}

}
