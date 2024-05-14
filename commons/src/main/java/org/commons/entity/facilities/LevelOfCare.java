package org.commons.entity.facilities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@Entity
@Table(name = "level_of_care")
public class LevelOfCare extends IdBasedEntity {
    @Column(length = 64, nullable = false)
    private String name;

    public LevelOfCare(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
