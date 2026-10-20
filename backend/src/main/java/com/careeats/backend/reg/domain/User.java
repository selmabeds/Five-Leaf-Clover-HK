
package com.careeats.backend.reg.domain;

public class User {

    private Long id;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;

    public User(Long id, String name, String email, String passwordHash, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = java.util.Objects.requireNonNull(role);
    }
    
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }
}
