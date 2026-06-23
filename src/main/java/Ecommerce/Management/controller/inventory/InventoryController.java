package Ecommerce.Management.controller.inventory;

import Ecommerce.Management.dto.inventory.InventoryAdjustRequest;
import Ecommerce.Management.dto.inventory.InventoryRequest;
import Ecommerce.Management.dto.inventory.InventoryResponse;
import Ecommerce.Management.dto.inventory.ProductStockResponse;
import Ecommerce.Management.service.inventory.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@GetMapping
	public List<InventoryResponse> listInventory(
			@RequestParam(required = false) Long warehouseId,
			@RequestParam(required = false) Long productId,
			@RequestParam(defaultValue = "true") boolean activeWarehousesOnly) {
		return inventoryService.listInventory(warehouseId, productId, activeWarehousesOnly);
	}

	@GetMapping("/{id}")
	public InventoryResponse getInventoryItem(@PathVariable Long id) {
		return inventoryService.getInventoryItem(id);
	}

	@GetMapping("/product/{productId}")
	public ProductStockResponse getProductStock(@PathVariable Long productId) {
		return inventoryService.getProductStock(productId);
	}

	@GetMapping("/warehouse/{warehouseId}")
	public List<InventoryResponse> getWarehouseInventory(@PathVariable Long warehouseId) {
		return inventoryService.getWarehouseInventory(warehouseId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InventoryResponse stockInventory(@Valid @RequestBody InventoryRequest request) {
		return inventoryService.stockInventory(request);
	}

	@PatchMapping("/adjust")
	public InventoryResponse adjustInventory(@Valid @RequestBody InventoryAdjustRequest request) {
		return inventoryService.adjustInventory(request);
	}

}
