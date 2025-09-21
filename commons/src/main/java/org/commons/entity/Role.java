package org.commons.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "role")
public class Role extends IdBasedEntity {
    @Column(length = 40, nullable = false, unique = true)
    private String name;
    @Column(length = 140, nullable = false)
    private String description;
    private boolean deleted;

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Role(String name) {
        this.name = name;
    }

    public Role(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
