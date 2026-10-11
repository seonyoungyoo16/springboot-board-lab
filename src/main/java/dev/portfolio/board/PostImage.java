package dev.portfolio.board;

import jakarta.persistence.*;

@Entity
@Table(name = "post_images")
public class PostImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false) Post post;
    @Column(nullable = false, length = 255) String filename;
    @Column(name = "content_type", nullable = false, length = 80) String contentType;
    @Lob @Column(nullable = false) byte[] content;
    protected PostImage() {}
    PostImage(Post post, String filename, String contentType, byte[] content) {
        this.post = post; this.filename = filename; this.contentType = contentType; this.content = content;
    }
}
