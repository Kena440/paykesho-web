package org.commons.entity.settings;

import lombok.*;
import org.commons.enums.SettingCategory;

import javax.persistence.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settings")
public class Setting {
    @Id
    @Column(name = "`key`", nullable = false, length = 128)
    private String key;
    @Column(nullable = false, length = 5024)
    private String value;
    @Enumerated(EnumType.STRING)
    @Column(length = 45, nullable = false)
    private SettingCategory category;

    public Setting(String key) {
        this.key = key;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Setting other = (Setting) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }
}
