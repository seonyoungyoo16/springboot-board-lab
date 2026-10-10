package dev.portfolio.board;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "display_name", nullable = false, length = 80) String displayName;
    protected AppUser() {}
    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
}
