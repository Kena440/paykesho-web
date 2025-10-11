package org.commons.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "api_keys")
public class APIKey extends IdBasedEntity {

    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;

    @Column(name = "application", length = 128)
    private String application;

    @Column(name = "api_key", length = 128)
    private String apiKey;

    @Column(name = "created_at", length = 128)
    private Date createdAt;
}
