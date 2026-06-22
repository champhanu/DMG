package Ecommerce.Management.repository.inventory;

import Ecommerce.Management.domain.inventory.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

	Optional<InventoryItem> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

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
