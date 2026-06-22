package Ecommerce.Management.service.audit;

import Ecommerce.Management.domain.audit.AuditLog;
import Ecommerce.Management.repository.audit.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

	private static final Logger log = LoggerFactory.getLogger(AuditService.class);

	private final AuditLogRepository auditLogRepository;

	public AuditService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(String eventType, String entityType, Long entityId, Long customerId, String message) {
		AuditLog entry = new AuditLog();
		entry.setEventType(eventType);
		entry.setEntityType(entityType);
		entry.setEntityId(entityId);
		entry.setCustomerId(customerId);
		entry.setMessage(message);
		auditLogRepository.save(entry);
		log.info("AUDIT {} entityType={} entityId={} customerId={} message={}",
				eventType, entityType, entityId, customerId, message);
	}

}
