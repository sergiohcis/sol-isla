package com.hosannasolutions.solisla.audit.providedService;

import com.hosannasolutions.solisla.audit.dto.AuditEventRequest;

public interface AuditService {

    void record(AuditEventRequest request);
}
