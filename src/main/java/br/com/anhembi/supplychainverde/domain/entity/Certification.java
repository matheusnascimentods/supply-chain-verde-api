package br.com.anhembi.supplychainverde.domain.entity;

import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certification {
    private Long certificationId;
    private Supplier supplier;
    private String certification;
    private String issuingBody;
    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private CertificationStatus status;
}
