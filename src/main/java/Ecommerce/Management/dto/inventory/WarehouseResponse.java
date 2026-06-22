package Ecommerce.Management.dto.inventory;

public record WarehouseResponse(
		Long id,
		String name,
		String code,
		String location,
		boolean active
) {
}
