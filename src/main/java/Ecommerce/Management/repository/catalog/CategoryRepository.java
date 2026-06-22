package Ecommerce.Management.repository.catalog;

import Ecommerce.Management.domain.catalog.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);

	Optional<Category> findBySlug(String slug);

	List<Category> findByParentIdAndActiveTrueOrderByNameAsc(Long parentId);

	List<Category> findByParentIdOrderByNameAsc(Long parentId);

	List<Category> findByParentIsNullAndActiveTrueOrderByNameAsc();

	List<Category> findByParentIsNullOrderByNameAsc();

}
