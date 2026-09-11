package com.hosannasolutions.solisla.audit.providedService;

import com.hosannasolutions.solisla.audit.AuditEvent;
import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;
import com.hosannasolutions.solisla.audit.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditServiceImpl(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Always REQUIRES_NEW: an audit write must survive regardless of whether the caller's
     * surrounding transaction (e.g. a failed login) later rolls back.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEventRequest request) {
        AuditEvent event = new AuditEvent(
                request.action(),
                request.userId(),
                request.entityType(),
                request.entityId(),
                currentRequest().map(HttpServletRequest::getRemoteAddr).orElse(null),
                currentRequest().map(r -> r.getHeader("User-Agent")).orElse(null),
                request.beforeData(),
                request.afterData()
        );
        auditEventRepository.save(event);
    }

    private Optional<HttpServletRequest> currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return Optional.empty();
        }
        return Optional.of(attrs.getRequest());
    }
}
