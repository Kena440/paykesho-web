package org.commons.entity.settings;

import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.entity.State;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

//@Data
@NoArgsConstructor
@Entity
@Table(name = "countries")
public class Country extends IdBasedEntity {
    @Column(length = 45, nullable = false)
    private String name;
    @Column(length = 5, nullable = false)
    private String code;
    @OneToMany(mappedBy = "country")
    private Set<State> states;

    public Country(Integer id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public Country(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public Country(String name) {
        this.name = name;
    }

    public Country(Integer countryId) {
        this.id = countryId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return name + " - " + code;
    }

    /*  public Set<State> getStates() {
        return states;
    }

    public void setStates(Set<State> states) {
        this.states = states;
    }*/
}
