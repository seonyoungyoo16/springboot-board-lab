package dev.portfolio.board;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false) Post post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false) AppUser author;
    @Column(nullable = false, length = 1000) String body;
    @Column(name = "created_at", nullable = false) Instant createdAt = Instant.now();
    protected Comment() {}
    Comment(Post post, AppUser author, String body) { this.post = post; this.author = author; this.body = body; }
}
