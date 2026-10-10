package dev.portfolio.board;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BoardController {
    private final BoardService service;
    BoardController(BoardService service) { this.service = service; }
    public record CreatePost(@NotBlank @Size(max = 120) String title, @NotBlank @Size(max = 10000) String body) {}
    public record UpdatePost(@NotBlank @Size(max = 120) String title, @NotBlank @Size(max = 10000) String body, @NotNull @PositiveOrZero Long version) {}
    public record CommentInput(@NotBlank @Size(max = 1000) String body) {}
    @GetMapping("/users") public List<BoardService.UserView> users() { return service.users(); }
    @GetMapping("/posts") public BoardService.PostPage list(@RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.list(actor, page, size); }
    @GetMapping("/posts/{id}") public BoardService.PostView detail(@PathVariable Long id, @RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor) { return service.detail(id, actor); }
    @PostMapping("/posts") @ResponseStatus(HttpStatus.CREATED) public BoardService.PostView create(@RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor, @Valid @RequestBody CreatePost input) { return service.create(actor, input.title(), input.body()); }
    @PutMapping("/posts/{id}") public BoardService.PostView update(@PathVariable Long id, @RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor, @Valid @RequestBody UpdatePost input) { return service.update(id, actor, input.title(), input.body(), input.version()); }
    @DeleteMapping("/posts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id, @RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor) { service.delete(id, actor); }
    @GetMapping("/posts/{id}/comments") public List<BoardService.CommentView> comments(@PathVariable Long id) { return service.comments(id); }
    @PostMapping("/posts/{id}/comments") @ResponseStatus(HttpStatus.CREATED) public BoardService.CommentView addComment(@PathVariable Long id, @RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor, @Valid @RequestBody CommentInput input) { return service.addComment(id, actor, input.body()); }
    @PutMapping("/posts/{id}/comments/{commentId}") public BoardService.CommentView updateComment(@PathVariable Long id, @PathVariable Long commentId, @RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor, @Valid @RequestBody CommentInput input) { return service.updateComment(id, commentId, actor, input.body()); }
    @DeleteMapping("/posts/{id}/comments/{commentId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteComment(@PathVariable Long id, @PathVariable Long commentId, @RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor) { service.deleteComment(id, commentId, actor); }
    @PutMapping("/posts/{id}/likes/me") public BoardService.PostView like(@PathVariable Long id, @RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor) { return service.like(id, actor); }
    @DeleteMapping("/posts/{id}/likes/me") public BoardService.PostView unlike(@PathVariable Long id, @RequestHeader(value = "X-Demo-User", defaultValue = "1") Long actor) { return service.unlike(id, actor); }
}
