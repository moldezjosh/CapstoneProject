package com.capstoneproject.capstone;

public class User {
    private String Email;
    private String Role;

    public User(String email, String role) {
        setEmail(email);
        setRole(role);
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getRole() {
        return Role;
    }

    public void setRole(String role) {
        Role = role;
    }
}
