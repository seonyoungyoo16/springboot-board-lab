package dev.portfolio.board;

import jakarta.persistence.*;

@Entity
@Table(name = "post_likes", uniqueConstraints = @UniqueConstraint(name = "uq_post_like", columnNames = {"post_id", "user_id"}))
public class PostLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false) Post post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) AppUser user;
    protected PostLike() {}
    PostLike(Post post, AppUser user) { this.post = post; this.user = user; }
}
