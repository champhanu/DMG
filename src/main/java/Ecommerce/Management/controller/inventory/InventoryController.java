package Ecommerce.Management.controller.inventory;

import Ecommerce.Management.dto.inventory.InventoryRequest;
import Ecommerce.Management.dto.inventory.InventoryResponse;
import Ecommerce.Management.dto.inventory.WarehouseRequest;
import Ecommerce.Management.dto.inventory.WarehouseResponse;
import Ecommerce.Management.service.inventory.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@PostMapping("/warehouses")
	@ResponseStatus(HttpStatus.CREATED)
	public WarehouseResponse createWarehouse(@Valid @RequestBody WarehouseRequest request) {
		return inventoryService.createWarehouse(request);
	}

	@PostMapping("/inventory")
	@ResponseStatus(HttpStatus.CREATED)
	public InventoryResponse stockInventory(@Valid @RequestBody InventoryRequest request) {
		return inventoryService.stockInventory(request);
	}

}
