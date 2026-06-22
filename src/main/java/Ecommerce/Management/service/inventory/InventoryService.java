package Ecommerce.Management.service.inventory;

import Ecommerce.Management.domain.catalog.Product;
import Ecommerce.Management.domain.inventory.InventoryItem;
import Ecommerce.Management.domain.inventory.Warehouse;
import Ecommerce.Management.dto.inventory.InventoryRequest;
import Ecommerce.Management.dto.inventory.InventoryResponse;
import Ecommerce.Management.dto.inventory.WarehouseRequest;
import Ecommerce.Management.dto.inventory.WarehouseResponse;
import Ecommerce.Management.exception.DuplicateResourceException;
import Ecommerce.Management.exception.InsufficientInventoryException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.inventory.InventoryItemRepository;
import Ecommerce.Management.repository.inventory.WarehouseRepository;
import Ecommerce.Management.service.catalog.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InventoryService {

	public record ReservationLine(Long warehouseId, int quantity) {
	}

	private final WarehouseRepository warehouseRepository;
	private final InventoryItemRepository inventoryItemRepository;
	private final ProductService productService;

	public InventoryService(
			WarehouseRepository warehouseRepository,
			InventoryItemRepository inventoryItemRepository,
			ProductService productService) {
		this.warehouseRepository = warehouseRepository;
		this.inventoryItemRepository = inventoryItemRepository;
		this.productService = productService;
	}

	public WarehouseResponse createWarehouse(WarehouseRequest request) {
		if (warehouseRepository.findByCode(request.code()).isPresent()) {
			throw new DuplicateResourceException("Warehouse code already exists: " + request.code());
		}
		Warehouse warehouse = new Warehouse();
		warehouse.setName(request.name());
		warehouse.setCode(request.code());
		warehouse.setLocation(request.location());
		return toWarehouseResponse(warehouseRepository.save(warehouse));
	}

	public InventoryResponse stockInventory(InventoryRequest request) {
		Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
				.orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.warehouseId()));
		Product product = productService.findActiveProduct(request.productId());

		InventoryItem item = inventoryItemRepository.findByWarehouseIdAndProductId(warehouse.getId(), product.getId())
				.orElseGet(() -> {
					InventoryItem created = new InventoryItem();
					created.setWarehouse(warehouse);
					created.setProduct(product);
					created.setQuantityAvailable(0);
					created.setQuantityReserved(0);
					return created;
				});

		item.setQuantityAvailable(item.getQuantityAvailable() + request.quantity());
		return toInventoryResponse(inventoryItemRepository.save(item));
	}

	public List<ReservationLine> reserveProduct(Long productId, int quantity) {
		List<InventoryItem> stockLines = inventoryItemRepository.findAvailableForProductWithLock(productId);
		int remaining = quantity;
		List<ReservationLine> reservations = new ArrayList<>();

		for (InventoryItem stock : stockLines) {
			if (remaining <= 0) {
				break;
			}
			int allocated = Math.min(remaining, stock.getQuantityAvailable());
			if (allocated <= 0) {
				continue;
			}
			stock.reserve(allocated);
			reservations.add(new ReservationLine(stock.getWarehouse().getId(), allocated));
			remaining -= allocated;
		}

		if (remaining > 0) {
			throw new InsufficientInventoryException(
					"Insufficient inventory for product " + productId + ". Short by " + remaining);
		}
		return reservations;
	}

	private WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
		return new WarehouseResponse(
				warehouse.getId(),
				warehouse.getName(),
				warehouse.getCode(),
				warehouse.getLocation(),
				warehouse.isActive());
	}

	private InventoryResponse toInventoryResponse(InventoryItem item) {
		return new InventoryResponse(
				item.getId(),
				item.getWarehouse().getId(),
				item.getWarehouse().getCode(),
				item.getProduct().getId(),
				item.getProduct().getSku(),
				item.getQuantityAvailable(),
				item.getQuantityReserved());
	}

}
