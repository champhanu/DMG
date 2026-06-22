package Ecommerce.Management.repository.inventory;

import Ecommerce.Management.domain.inventory.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

	@EntityGraph(attributePaths = { "warehouse", "product" })
	Optional<InventoryItem> findById(Long id);

	Optional<InventoryItem> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

	@EntityGraph(attributePaths = { "warehouse", "product" })
	List<InventoryItem> findByWarehouseIdOrderByProduct_NameAsc(Long warehouseId);

	@EntityGraph(attributePaths = { "warehouse", "product" })
	List<InventoryItem> findByProductIdOrderByWarehouse_CodeAsc(Long productId);

	@EntityGraph(attributePaths = { "warehouse", "product" })
	@Query("""
			SELECT i FROM InventoryItem i
			WHERE (:warehouseId IS NULL OR i.warehouse.id = :warehouseId)
			  AND (:productId IS NULL OR i.product.id = :productId)
			  AND (:activeWarehousesOnly = false OR i.warehouse.active = true)
			ORDER BY i.warehouse.code ASC, i.product.name ASC
			""")
	List<InventoryItem> search(
			@Param("warehouseId") Long warehouseId,
			@Param("productId") Long productId,
			@Param("activeWarehousesOnly") boolean activeWarehousesOnly);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT i FROM InventoryItem i
			WHERE i.product.id = :productId
			  AND i.quantityAvailable > 0
			  AND i.warehouse.active = true
			ORDER BY i.quantityAvailable DESC
			""")
	List<InventoryItem> findAvailableForProductWithLock(@Param("productId") Long productId);

}
