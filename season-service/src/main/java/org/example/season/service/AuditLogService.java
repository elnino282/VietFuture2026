package org.example.season.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@Slf4j
public class AuditLogService {

    public void logModuleOperation(
            String module,
            String subModule,
            Object targetId,
            String operation,
            String actor,
            Map<String, Object> snapshot,
            Object detail1,
            Object detail2) {
        log.info("AUDIT LOG - Module: {}, SubModule: {}, TargetId: {}, Operation: {}, Actor: {}, Snapshot: {}",
                module, subModule, targetId, operation, actor, snapshot);
    }
}
