package Ecommerce.Management.domain.fulfillment;

import Ecommerce.Management.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "fulfillment_tasks")
public class FulfillmentTask extends BaseEntity {

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Column(name = "warehouse_id", nullable = false)
	private Long warehouseId;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(nullable = false)
	private int quantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FulfillmentTaskStatus status = FulfillmentTaskStatus.PENDING;

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public Long getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(Long warehouseId) {
		this.warehouseId = warehouseId;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public FulfillmentTaskStatus getStatus() {
		return status;
	}

	public void setStatus(FulfillmentTaskStatus status) {
		this.status = status;
	}

}
