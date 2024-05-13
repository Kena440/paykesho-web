package org.membershipcommons.entity.settings;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.membershipcommons.entity.IdBasedEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@ToString
@Table(name = "need_types")
@Entity
@EqualsAndHashCode(exclude = "parent")
public class NeedType extends IdBasedEntity {
    @Column(length = 64, nullable = false)
    private String name;
    @Column(length = 64, nullable = false, unique = true)
    private String alias;
    @Column(length = 255, nullable = false, unique = true)
    private String description;
    private boolean enabled;

    @Transient
    private boolean hasChildren;
    @Column(name = "all_parent_ids", length = 255, nullable = true)
    private String allParentIds;
    @OneToOne
    @JoinColumn(name = "parent_id")
    private NeedType parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("name asc")
    @ToString.Exclude
    private Set<NeedType> children = new HashSet<>();

    public NeedType(String name) {
        this.name = name;
        this.alias = name;
    }

    public NeedType(String name, NeedType parent) {
        this(name);
        this.parent = parent;
    }

    public NeedType(String name, Integer id) {
        this.name = name;
        this.id = id;
    }

    public NeedType(Integer id) {
        this.id = id;
    }

    public NeedType(Integer id, String name, String alias) {
        this.name = name;
        this.id = id;
        this.alias = alias;
    }

    public NeedType() {

    }

    public NeedType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public NeedType(String name, String alias, String description) {
        this.name = name;
        this.alias = alias;
        this.description = description;
    }

    public static NeedType copyIdAndName(NeedType category) {
        NeedType copyNeedType = new NeedType();
        copyNeedType.setId(category.getId());
        copyNeedType.setName(category.getName());

        return copyNeedType;
    }

    public static NeedType copyIdAndName(Integer id, String name) {
        NeedType copyNeedType = new NeedType();
        copyNeedType.setId(id);
        copyNeedType.setName(name);

        return copyNeedType;
    }

    public static NeedType copyFull(NeedType category) {
        NeedType copyNeedType = new NeedType();
        copyNeedType.setId(category.getId());
        copyNeedType.setName(category.getName());
        copyNeedType.setAlias(category.getAlias());
        copyNeedType.setHasChildren(category.getChildren().size() > 0);

        return copyNeedType;
    }

    public static NeedType copyFull(NeedType category, String name) {
        NeedType copyNeedType = NeedType.copyFull(category);
        copyNeedType.setName(name);

        return copyNeedType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        NeedType category = (NeedType) o;
        return id != null && Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Transient
    public String getParentName() {
        return parent.name;
    }

    @Override
    public String toString() {
        return name;
    }
}
