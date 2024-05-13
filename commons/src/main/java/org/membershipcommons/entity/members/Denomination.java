package org.membershipcommons.entity.members;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@Entity
@Table(name = "denominations")
public class Denomination extends IdBasedEntity {
    @Column(length = 64, nullable = false)
    private String name;

    public Denomination(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
