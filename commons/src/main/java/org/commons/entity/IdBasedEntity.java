package org.commons.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.UUID;

@Data
@MappedSuperclass
public abstract class IdBasedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Integer id;
    
    @Column(name = "uuid", unique = true, nullable = false, updatable = false, length = 36)
    private String uuid;
    
    /**
     * Default constructor - generates UUID immediately upon instantiation
     */
    public IdBasedEntity() {
        generateUuid();
    }
    
    /**
     * Method to ensure UUID is generated (can be called explicitly if needed)
     */
    public void ensureUuid() {
        generateUuid();
    }
    
    @PrePersist
    public void generateUuid() {
        if (this.uuid == null || this.uuid.trim().isEmpty()) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
