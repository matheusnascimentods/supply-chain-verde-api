package br.com.anhembi.supplychainverde.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "number", nullable = false)
    private String number;

    @Column(name = "neighborhood", nullable = false)
    private String neighborhood;

    @Column(name = "complement")
    private String complement;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;
}
