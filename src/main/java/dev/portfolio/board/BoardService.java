package dev.portfolio.board;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BoardService {
    private final UserRepository users;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final LikeRepository likes;
    BoardService(UserRepository users, PostRepository posts, CommentRepository comments, LikeRepository likes) {
        this.users = users; this.posts = posts; this.comments = comments; this.likes = likes;
    }
    public record UserView(Long id, String displayName) {}
    public record PostView(Long id, Long authorId, String authorName, String title, String body, Instant createdAt, Long version, long commentCount, long likeCount, boolean liked) {}
    public record CommentView(Long id, Long postId, Long authorId, String authorName, String body, Instant createdAt) {}
    public record PostPage(List<PostView> content, long totalElements, int totalPages, int page) {}
    private AppUser user(Long id) { return users.findById(id).orElseThrow(() -> missing("사용자")); }
    private Post post(Long id) { return posts.findById(id).orElseThrow(() -> missing("게시글")); }
    private Post locked(Long id) { return posts.findLocked(id).orElseThrow(() -> missing("게시글")); }
    private ResponseStatusException missing(String name) { return new ResponseStatusException(HttpStatus.NOT_FOUND, name + "을 찾을 수 없습니다."); }
    private void owner(Long authorId, Long actor) { user(actor); if (!authorId.equals(actor)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 변경할 수 있습니다."); }
    private PostView view(Post p, Long actor) { return new PostView(p.id, p.author.getId(), p.author.getDisplayName(), p.title, p.body, p.createdAt, p.version, comments.countByPostId(p.id), likes.countByPostId(p.id), likes.existsByPostIdAndUserId(p.id, actor)); }
    private CommentView view(Comment c) { return new CommentView(c.id, c.post.getId(), c.author.getId(), c.author.getDisplayName(), c.body, c.createdAt); }
    @Transactional(readOnly = true)
    public List<UserView> users() { return users.findAll().stream().map(u -> new UserView(u.id, u.displayName)).toList(); }
    @Transactional(readOnly = true)
    public PostPage list(Long actor, int page, int size) {
        user(actor);
        var result = posts.findAll(PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 50), Sort.by(Sort.Direction.DESC, "id")));
        return new PostPage(result.stream().map(p -> view(p, actor)).toList(), result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }
    @Transactional(readOnly = true)
    public PostView detail(Long id, Long actor) { user(actor); return view(post(id), actor); }
    public PostView create(Long actor, String title, String body) { return view(posts.saveAndFlush(new Post(user(actor), title.trim(), body.trim())), actor); }
    public PostView update(Long id, Long actor, String title, String body, Long version) {
        var p = locked(id); owner(p.author.getId(), actor);
        if (!p.version.equals(version)) throw new ResponseStatusException(HttpStatus.CONFLICT, "글이 변경되었습니다. 다시 조회해 주세요.");
        p.title = title.trim(); p.body = body.trim(); posts.flush(); return view(p, actor);
    }
    public void delete(Long id, Long actor) {
        var p = locked(id); owner(p.author.getId(), actor);
        comments.deleteByPostId(id); likes.deleteByPostId(id); comments.flush(); likes.flush(); posts.delete(p); posts.flush();
    }
    @Transactional(readOnly = true)
    public List<CommentView> comments(Long id) { post(id); return comments.findByPostIdOrderByIdAsc(id).stream().map(this::view).toList(); }
    public CommentView addComment(Long id, Long actor, String body) { var p = locked(id); return view(comments.saveAndFlush(new Comment(p, user(actor), body.trim()))); }
    public CommentView updateComment(Long id, Long commentId, Long actor, String body) { locked(id); var c = comment(id, commentId); owner(c.author.getId(), actor); c.body = body.trim(); comments.flush(); return view(c); }
    public void deleteComment(Long id, Long commentId, Long actor) { locked(id); var c = comment(id, commentId); owner(c.author.getId(), actor); comments.delete(c); }
    private Comment comment(Long postId, Long id) { var c = comments.findById(id).orElseThrow(() -> missing("댓글")); if (!c.post.getId().equals(postId)) throw missing("댓글"); return c; }
    public PostView like(Long id, Long actor) { var p = locked(id); var u = user(actor); if (!likes.existsByPostIdAndUserId(id, actor)) likes.saveAndFlush(new PostLike(p, u)); return view(p, actor); }
    public PostView unlike(Long id, Long actor) { var p = locked(id); user(actor); likes.findByPostIdAndUserId(id, actor).ifPresent(likes::delete); likes.flush(); return view(p, actor); }
}
