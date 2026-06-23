package Ecommerce.Management.domain.cart;

import Ecommerce.Management.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {

	@Column(name = "customer_id", nullable = false)
	private Long customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CartStatus status = CartStatus.ACTIVE;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CartItem> items = new ArrayList<>();

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public CartStatus getStatus() {
		return status;
	}

	public void setStatus(CartStatus status) {
		this.status = status;
	}

	public List<CartItem> getItems() {
		return items;
	}

	public void addItem(CartItem item) {
		items.add(item);
		item.setCart(this);
	}

	public void removeItem(CartItem item) {
		items.remove(item);
		item.setCart(null);
	}

}
