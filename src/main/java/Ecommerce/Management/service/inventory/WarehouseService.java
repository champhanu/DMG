package Ecommerce.Management.service.inventory;

import Ecommerce.Management.domain.inventory.Warehouse;
import Ecommerce.Management.dto.inventory.UpdateWarehouseRequest;
import Ecommerce.Management.dto.inventory.WarehouseRequest;
import Ecommerce.Management.dto.inventory.WarehouseResponse;
import Ecommerce.Management.exception.DuplicateResourceException;
import Ecommerce.Management.exception.InvalidOperationException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.inventory.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WarehouseService {

	private final WarehouseRepository warehouseRepository;

	public WarehouseService(WarehouseRepository warehouseRepository) {
		this.warehouseRepository = warehouseRepository;
	}

	public WarehouseResponse createWarehouse(WarehouseRequest request) {
		if (warehouseRepository.findByCode(request.code()).isPresent()) {
			throw new DuplicateResourceException("Warehouse code already exists: " + request.code());
		}
		Warehouse warehouse = new Warehouse();
		warehouse.setName(request.name());
		warehouse.setCode(request.code().toUpperCase());
		warehouse.setLocation(request.location());
		return toResponse(warehouseRepository.save(warehouse));
	}

	public WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request) {
		Warehouse warehouse = findWarehouse(id);
		warehouse.setName(request.name());
		warehouse.setLocation(request.location());
		return toResponse(warehouse);
	}

	public void deactivateWarehouse(Long id) {
		Warehouse warehouse = findWarehouse(id);
		if (!warehouse.isActive()) {
			return;
		}
		warehouse.setActive(false);
	}

	@Transactional(readOnly = true)
	public WarehouseResponse getWarehouse(Long id) {
		return toResponse(findWarehouse(id));
	}

	@Transactional(readOnly = true)
	public List<WarehouseResponse> listWarehouses(boolean includeInactive) {
		List<Warehouse> warehouses = includeInactive
				? warehouseRepository.findAllByOrderByNameAsc()
				: warehouseRepository.findByActiveTrueOrderByNameAsc();
		return warehouses.stream().map(this::toResponse).toList();
	}

	public Warehouse findActiveWarehouse(Long id) {
		Warehouse warehouse = findWarehouse(id);
		if (!warehouse.isActive()) {
			throw new InvalidOperationException("Warehouse is inactive: " + id);
		}
		return warehouse;
	}

	private Warehouse findWarehouse(Long id) {
		return warehouseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
	}

	private WarehouseResponse toResponse(Warehouse warehouse) {
		return new WarehouseResponse(
				warehouse.getId(),
				warehouse.getName(),
				warehouse.getCode(),
				warehouse.getLocation(),
				warehouse.isActive(),
				warehouse.getCreatedAt(),
				warehouse.getUpdatedAt());
	}

}
