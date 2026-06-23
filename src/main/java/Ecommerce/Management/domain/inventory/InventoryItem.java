package Ecommerce.Management.domain.inventory;

import Ecommerce.Management.domain.BaseEntity;
import Ecommerce.Management.domain.catalog.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_items", uniqueConstraints = {
		@UniqueConstraint(name = "uk_inventory_warehouse_product", columnNames = { "warehouse_id", "product_id" })
})
public class InventoryItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "quantity_available", nullable = false)
	private int quantityAvailable;

	@Column(name = "quantity_reserved", nullable = false)
	private int quantityReserved;

	@Version
	private long version;

	public Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public int getQuantityAvailable() {
		return quantityAvailable;
	}

	public void setQuantityAvailable(int quantityAvailable) {
		this.quantityAvailable = quantityAvailable;
	}

	public int getQuantityReserved() {
		return quantityReserved;
	}

	public void setQuantityReserved(int quantityReserved) {
		this.quantityReserved = quantityReserved;
	}

	public void reserve(int quantity) {
		if (quantityAvailable < quantity) {
			throw new IllegalStateException("Insufficient stock in warehouse " + warehouse.getCode());
		}
		quantityAvailable -= quantity;
		quantityReserved += quantity;
	}

	public void release(int quantity) {
		if (quantityReserved < quantity) {
			throw new IllegalStateException("Cannot release more than reserved in warehouse " + warehouse.getCode());
		}
		quantityReserved -= quantity;
		quantityAvailable += quantity;
	}

}
