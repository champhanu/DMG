package Ecommerce.Management.service.catalog;

import Ecommerce.Management.domain.catalog.Category;
import Ecommerce.Management.dto.catalog.CategoryRequest;
import Ecommerce.Management.dto.catalog.CategoryResponse;
import Ecommerce.Management.exception.DuplicateResourceException;
import Ecommerce.Management.exception.InvalidOperationException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.catalog.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> listCategories(Long parentId, boolean includeInactive) {
		List<Category> categories;
		if (parentId != null) {
			categories = includeInactive
					? categoryRepository.findByParentIdOrderByNameAsc(parentId)
					: categoryRepository.findByParentIdAndActiveTrueOrderByNameAsc(parentId);
		}
		else {
			categories = includeInactive
					? categoryRepository.findByParentIsNullOrderByNameAsc()
					: categoryRepository.findByParentIsNullAndActiveTrueOrderByNameAsc();
		}
		return categories.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse getCategory(Long id) {
		return toResponse(findCategory(id));
	}

	public CategoryResponse createCategory(CategoryRequest request) {
		if (categoryRepository.existsBySlug(request.slug())) {
			throw new DuplicateResourceException("Category slug already exists: " + request.slug());
		}
		Category category = new Category();
		applyRequest(category, request);
		return toResponse(categoryRepository.save(category));
	}

	public CategoryResponse updateCategory(Long id, CategoryRequest request) {
		Category category = findCategory(id);
		if (categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
			throw new DuplicateResourceException("Category slug already exists: " + request.slug());
		}
		applyRequest(category, request);
		return toResponse(category);
	}

	public void deactivateCategory(Long id) {
		Category category = findCategory(id);
		if (!category.isActive()) {
			return;
		}
		category.setActive(false);
	}

	public Category findActiveCategory(Long id) {
		Category category = findCategory(id);
		if (!category.isActive()) {
			throw new InvalidOperationException("Category is inactive: " + id);
		}
		return category;
	}

	private void applyRequest(Category category, CategoryRequest request) {
		category.setName(request.name());
		category.setSlug(request.slug());
		category.setDescription(request.description());
		if (request.parentId() != null) {
			if (category.getId() != null && category.getId().equals(request.parentId())) {
				throw new InvalidOperationException("Category cannot be its own parent");
			}
			Category parent = findCategory(request.parentId());
			if (!parent.isActive()) {
				throw new InvalidOperationException("Parent category is inactive: " + request.parentId());
			}
			category.setParent(parent);
		}
		else {
			category.setParent(null);
		}
	}

	private Category findCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
	}

	private CategoryResponse toResponse(Category category) {
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				category.getSlug(),
				category.getDescription(),
				category.getParent() != null ? category.getParent().getId() : null,
				category.isActive(),
				category.getCreatedAt(),
				category.getUpdatedAt());
	}

}
