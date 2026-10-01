package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.AuditAction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long logId;
    private User user;
    private AuditAction action;
    private String affectedTable;
    private Long affectedEntityId;
    private Map<String, Object> beforeData;
    private Map<String, Object> afterData;
    private LocalDateTime performedAt;
}
