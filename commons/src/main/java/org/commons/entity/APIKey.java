package org.commons.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "api_keys")
public class APIKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "reference", length = 128, nullable = false, unique = true)
    private String reference;
    @Column(name = "application", length = 128)
    private String application;
    @Column(name = "key", length = 128)
    private String key;
    @Column(name = "created_at", length = 128)
    private Date createdAt;
}
