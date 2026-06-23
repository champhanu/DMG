package Ecommerce.Management.domain.audit;

import Ecommerce.Management.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

	@Column(name = "event_type", nullable = false, length = 64)
	private String eventType;

	@Column(name = "entity_type", length = 64)
	private String entityType;

	@Column(name = "entity_id")
	private Long entityId;

	@Column(name = "customer_id")
	private Long customerId;

	@Column(nullable = false, length = 1000)
	private String message;

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
