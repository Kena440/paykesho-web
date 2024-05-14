package org.commons.entity.settings;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@Entity
@Table(name = "currencies")
public class Currency extends IdBasedEntity {
    @Column(length = 64, nullable = false)
    private String name;
    @Column(length = 3, nullable = false)
    private String symbol;
    @Column(length = 4, nullable = false)
    private String code;

    public Currency(String name, String symbol, String code) {
        this.name = name;
        this.symbol = symbol;
        this.code = code;
    }

    @Override
    public String toString() {
        return name + " ~ " + symbol + " " + code;
    }
}
