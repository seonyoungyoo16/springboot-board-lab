package dev.portfolio.board;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false) AppUser author;
    @Column(nullable = false, length = 120) String title;
    @Column(nullable = false, length = 10000) String body;
    @Column(name = "created_at", nullable = false) Instant createdAt = Instant.now();
    @Version @Column(nullable = false) Long version;
    protected Post() {}
    public Long getId() { return id; }
    Post(AppUser author, String title, String body) { this.author = author; this.title = title; this.body = body; }
}
