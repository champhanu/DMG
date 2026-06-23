package Ecommerce.Management.repository.catalog;

import Ecommerce.Management.domain.catalog.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, Long id);

	@Query("""
			SELECT p FROM Product p
			WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
			  AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
			       OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))
			  AND (:includeInactive = true OR p.active = true)
			""")
	Page<Product> search(
			@Param("categoryId") Long categoryId,
			@Param("search") String search,
			@Param("includeInactive") boolean includeInactive,
			Pageable pageable);

}
