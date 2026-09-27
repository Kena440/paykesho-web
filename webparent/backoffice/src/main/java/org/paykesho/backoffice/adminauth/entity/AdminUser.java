package org.paykesho.backoffice.adminauth.entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class AdminUser {
    @Id
    private Integer id;
    @Column(name="email", nullable=false, unique=true)
    private String email;
    @Column(name="password", nullable=false)
    private String password;
    @Column(name="first_name") private String firstName;
    @Column(name="last_name") private String lastName;
    @Column(name="enabled") private boolean enabled;
    @Column(name="deleted") private boolean deleted;
    @Column(name="account_non_locked") private boolean accountNonLocked;
    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name="user_roles",
        joinColumns=@JoinColumn(name="user_id"),
        inverseJoinColumns=@JoinColumn(name="role_id"))
    private Set<AdminRole> roles = new HashSet<>();
    public Integer getId(){return id;}
    public void setId(Integer id){this.id=id;}
    public String getEmail(){return email;}
    public void setEmail(String email){this.email=email;}
    public String getPassword(){return password;}
    public void setPassword(String password){this.password=password;}
    public String getFirstName(){return firstName;}
    public String getLastName(){return lastName;}
    public boolean isEnabled(){return enabled;}
    public boolean isDeleted(){return deleted;}
    public boolean isAccountNonLocked(){return accountNonLocked;}
    public Set<AdminRole> getRoles(){return roles;}
}
