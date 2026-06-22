package Ecommerce.Management.controller.inventory;

import Ecommerce.Management.dto.inventory.UpdateWarehouseRequest;
import Ecommerce.Management.dto.inventory.WarehouseRequest;
import Ecommerce.Management.dto.inventory.WarehouseResponse;
import Ecommerce.Management.service.inventory.WarehouseService;
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
@RequestMapping("/api/warehouses")
public class WarehouseController {

	private final WarehouseService warehouseService;

	public WarehouseController(WarehouseService warehouseService) {
		this.warehouseService = warehouseService;
	}

	@GetMapping
	public List<WarehouseResponse> listWarehouses(
			@RequestParam(defaultValue = "false") boolean includeInactive) {
		return warehouseService.listWarehouses(includeInactive);
	}

	@GetMapping("/{id}")
	public WarehouseResponse getWarehouse(@PathVariable Long id) {
		return warehouseService.getWarehouse(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WarehouseResponse createWarehouse(@Valid @RequestBody WarehouseRequest request) {
		return warehouseService.createWarehouse(request);
	}

	@PutMapping("/{id}")
	public WarehouseResponse updateWarehouse(
			@PathVariable Long id,
			@Valid @RequestBody UpdateWarehouseRequest request) {
		return warehouseService.updateWarehouse(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateWarehouse(@PathVariable Long id) {
		warehouseService.deactivateWarehouse(id);
	}

}
