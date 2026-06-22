package Ecommerce.Management.service.inventory;

import Ecommerce.Management.domain.catalog.Product;
import Ecommerce.Management.domain.inventory.InventoryItem;
import Ecommerce.Management.domain.inventory.Warehouse;
import Ecommerce.Management.dto.inventory.InventoryAdjustRequest;
import Ecommerce.Management.dto.inventory.InventoryRequest;
import Ecommerce.Management.dto.inventory.InventoryResponse;
import Ecommerce.Management.dto.inventory.ProductStockResponse;
import Ecommerce.Management.exception.InsufficientInventoryException;
import Ecommerce.Management.exception.InvalidOperationException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.inventory.InventoryItemRepository;
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

	private final InventoryItemRepository inventoryItemRepository;
	private final WarehouseService warehouseService;
	private final ProductService productService;

	public InventoryService(
			InventoryItemRepository inventoryItemRepository,
			WarehouseService warehouseService,
			ProductService productService) {
		this.inventoryItemRepository = inventoryItemRepository;
		this.warehouseService = warehouseService;
		this.productService = productService;
	}

	public InventoryResponse stockInventory(InventoryRequest request) {
		Warehouse warehouse = warehouseService.findActiveWarehouse(request.warehouseId());
		Product product = productService.findActiveProduct(request.productId());
		InventoryItem item = getOrCreateInventoryItem(warehouse, product);
		item.setQuantityAvailable(item.getQuantityAvailable() + request.quantity());
		return toResponse(inventoryItemRepository.save(item));
	}

	public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
		Warehouse warehouse = warehouseService.findActiveWarehouse(request.warehouseId());
		Product product = productService.findActiveProduct(request.productId());
		InventoryItem item = inventoryItemRepository.findByWarehouseIdAndProductId(warehouse.getId(), product.getId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"No inventory record for product " + product.getId() + " in warehouse " + warehouse.getId()));

		int newAvailable = item.getQuantityAvailable() + request.delta();
		if (newAvailable < 0) {
			throw new InvalidOperationException("Adjustment would make available quantity negative");
		}
		item.setQuantityAvailable(newAvailable);
		return toResponse(item);
	}

	@Transactional(readOnly = true)
	public InventoryResponse getInventoryItem(Long id) {
		return toResponse(findInventoryItem(id));
	}

	@Transactional(readOnly = true)
	public List<InventoryResponse> listInventory(Long warehouseId, Long productId, boolean activeWarehousesOnly) {
		return inventoryItemRepository.search(warehouseId, productId, activeWarehousesOnly).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<InventoryResponse> getWarehouseInventory(Long warehouseId) {
		warehouseService.getWarehouse(warehouseId);
		return inventoryItemRepository.findByWarehouseIdOrderByProduct_NameAsc(warehouseId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ProductStockResponse getProductStock(Long productId) {
		Product product = productService.findActiveProduct(productId);
		List<InventoryResponse> lines = inventoryItemRepository.findByProductIdOrderByWarehouse_CodeAsc(productId).stream()
				.filter(item -> item.getWarehouse().isActive())
				.map(this::toResponse)
				.toList();

		int totalAvailable = lines.stream().mapToInt(InventoryResponse::quantityAvailable).sum();
		int totalReserved = lines.stream().mapToInt(InventoryResponse::quantityReserved).sum();

		return new ProductStockResponse(
				product.getId(),
				product.getName(),
				product.getSku(),
				totalAvailable,
				totalReserved,
				totalAvailable + totalReserved,
				lines);
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

	private InventoryItem getOrCreateInventoryItem(Warehouse warehouse, Product product) {
		return inventoryItemRepository.findByWarehouseIdAndProductId(warehouse.getId(), product.getId())
				.orElseGet(() -> {
					InventoryItem created = new InventoryItem();
					created.setWarehouse(warehouse);
					created.setProduct(product);
					created.setQuantityAvailable(0);
					created.setQuantityReserved(0);
					return created;
				});
	}

	private InventoryItem findInventoryItem(Long id) {
		return inventoryItemRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + id));
	}

	private InventoryResponse toResponse(InventoryItem item) {
		return new InventoryResponse(
				item.getId(),
				item.getWarehouse().getId(),
				item.getWarehouse().getCode(),
				item.getWarehouse().getName(),
				item.getProduct().getId(),
				item.getProduct().getName(),
				item.getProduct().getSku(),
				item.getQuantityAvailable(),
				item.getQuantityReserved(),
				item.getQuantityAvailable() + item.getQuantityReserved(),
				item.getUpdatedAt());
	}

}
