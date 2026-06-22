package Ecommerce.Management.repository.audit;

import Ecommerce.Management.domain.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
