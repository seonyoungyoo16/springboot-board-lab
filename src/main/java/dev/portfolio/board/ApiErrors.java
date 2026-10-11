package dev.portfolio.board;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ProblemDetail uploadTooLarge(Exception error) { return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "이미지는 2MB 이하로 첨부해 주세요."); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요. 제목은 120자, 본문은 10000자, 댓글은 1000자까지 입력할 수 있습니다.");
    }
}
