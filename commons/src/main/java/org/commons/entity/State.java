package org.commons.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.commons.entity.settings.Country;

import javax.persistence.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@Entity
@Table(name = "states")
public class State extends IdBasedEntity {
    @Column(length = 45, nullable = false)
    private String name;
    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    public State(String name, Country country) {
        this.name = name;
        this.country = country;
    }


    @Override
    public String toString() {
        return name + ", " + country.getCode();
    }
}
