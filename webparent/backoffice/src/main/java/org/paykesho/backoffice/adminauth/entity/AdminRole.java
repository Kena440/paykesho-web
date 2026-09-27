package org.paykesho.backoffice.adminauth.entity;

import javax.persistence.*;

@Entity
@Table(name = "role")
public class AdminRole {
    @Id
    private Integer id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "deleted")
    private boolean deleted;
    public Integer getId(){return id;}
    public void setId(Integer id){this.id=id;}
    public String getName(){return name;}
    public void setName(String name){this.name=name;}
    public String getDescription(){return description;}
    public void setDescription(String description){this.description=description;}
    public boolean isDeleted(){return deleted;}
    public void setDeleted(boolean deleted){this.deleted=deleted;}
}
