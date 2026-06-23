package Ecommerce.Management.domain.discount;

import Ecommerce.Management.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "discounts")
public class Discount extends BaseEntity {

	@Column(nullable = false, unique = true, length = 32)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DiscountType type;

	@Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
	private BigDecimal value;

	@Column(name = "min_order_amount", precision = 12, scale = 2)
	private BigDecimal minOrderAmount;

	@Column(name = "max_uses")
	private Integer maxUses;

	@Column(name = "used_count", nullable = false)
	private int usedCount;

	@Column(nullable = false)
	private boolean active = true;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public DiscountType getType() {
		return type;
	}

	public void setType(DiscountType type) {
		this.type = type;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public BigDecimal getMinOrderAmount() {
		return minOrderAmount;
	}

	public void setMinOrderAmount(BigDecimal minOrderAmount) {
		this.minOrderAmount = minOrderAmount;
	}

	public Integer getMaxUses() {
		return maxUses;
	}

	public void setMaxUses(Integer maxUses) {
		this.maxUses = maxUses;
	}

	public int getUsedCount() {
		return usedCount;
	}

	public void setUsedCount(int usedCount) {
		this.usedCount = usedCount;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
