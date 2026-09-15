package br.com.anhembi.supplychainverde.domain.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private Long addressId;
    private String street;
    private String number;
    private String neighborhood;
    private String complement;
    private String zipCode;
    private String city;
    private String state;
}
