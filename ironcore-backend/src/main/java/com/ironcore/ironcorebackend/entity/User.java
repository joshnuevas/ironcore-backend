package com.ironcore.ironcorebackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    // NEW FIELD FOR ADMIN STATUS
    // Explicitly map Java camelCase to database snake_case
    @Column(name = "is_admin", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isAdmin = false;

    // PROFILE PICTURE FIELDS
    @Lob
    @Column(name = "profile_picture", columnDefinition = "LONGBLOB")
    private byte[] profilePicture;

    @Column(name = "profile_picture_mime_type", length = 50)
    private String profilePictureMimeType;

    // Constructors
    public User() {
        this.isAdmin = false;
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.isAdmin = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getProfilePictureMimeType() {
        return profilePictureMimeType;
    }

    public void setProfilePictureMimeType(String profilePictureMimeType) {
        this.profilePictureMimeType = profilePictureMimeType;
    }
}