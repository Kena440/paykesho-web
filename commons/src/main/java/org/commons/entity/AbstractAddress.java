package org.commons.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@Data
@EqualsAndHashCode(callSuper = false)
@MappedSuperclass
public abstract class AbstractAddress extends IdBasedEntity {
    @Column(length = 45, nullable = false, name = "first_name")
    protected String firstName;
    @Column(length = 45, nullable = false, name = "last_name")
    protected String lastName;
    @Column(length = 15, nullable = false, name = "phone_number")
    protected String phoneNumber;
    @Column(length = 64, nullable = false, name = "address_line1")
    protected String addressLine1;
    @Column(length = 64, nullable = false, name = "address_line2")
    protected String addressLine2;
    @Column(nullable = false, name = "city")
    protected String city;
    @Column(nullable = false, name = "state")
    protected String state;
    @Column(length = 10, nullable = false, name = "postal_code")
    protected String postalCode;
}
