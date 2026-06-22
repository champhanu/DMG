package Ecommerce.Management.domain.inventory;

import Ecommerce.Management.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "warehouses", uniqueConstraints = {
		@UniqueConstraint(name = "uk_warehouses_code", columnNames = "code")
})
public class Warehouse extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 32)
	private String code;

	@Column(length = 255)
	private String location;

	@Column(nullable = false)
	private boolean active = true;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
