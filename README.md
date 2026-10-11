# Spring Boot Board Lab

Spring Boot 3.5.16 · Java 21 · Spring Data JPA · H2로 만든 간단한 게시판입니다. 글 작성·조회·수정·삭제, 댓글 작성·수정·삭제, 사용자별 좋아요 등록·취소를 지원합니다.

## 실행
Windows(Java 21 필요):
```powershell
.\scripts\maven.ps1 spring-boot:run
```
Maven이 설치된 환경:
```sh
mvn spring-boot:run
```
http://localhost:8088 에서 실제 API를 사용하는 UI가 열립니다. H2 파일 DB는 `data/`에 저장합니다.

## 검증과 패키지
```powershell
.\scripts\maven.ps1 verify
java -jar target/springboot-board-lab-1.0.0.jar
```
```sh
docker build -t board-lab .
docker run --rm -p 8088:8088 -v board-data:/app/data board-lab
```
GitHub Actions 설정은 `docs/ci-template.yml`에 제공합니다. 현재 GitHub 연결의 workflow 권한이 없어 활성화하지 않았습니다. `.github/workflows/ci.yml`로 옮겨 등록하면 테스트 후 실행 JAR를 artifact로 제공합니다. GitHub에 소스를 게시하는 것과 Java 서버를 클라우드에서 실행하는 것은 별개입니다.

## 설계
- `app_users` 1:N `posts` (`author_id`)
- `posts` 1:N `comments` (`post_id`), `app_users` 1:N `comments` (`author_id`)
- `posts` 1:N `post_likes` (`post_id`), `app_users` 1:N `post_likes` (`user_id`)
- `post_likes`는 사용자와 게시글의 N:M 관계를 연결하는 테이블이며 `(post_id,user_id)`가 유일합니다.
- 좋아요 PUT/DELETE는 멱등적입니다. 글 잠금과 유일 제약으로 같은 사용자의 중복을 막습니다.
- 글 수정 시 현재 `version`이 필요하며 오래된 버전은 409입니다.
- 글 삭제는 하나의 트랜잭션에서 댓글 → 좋아요 → 게시글 순으로 삭제합니다. DB FK는 유지합니다.
- 작성자만 자신의 글·댓글을 변경할 수 있습니다. 다른 글의 댓글 ID를 보내면 404입니다.

`X-Demo-User: 1` 또는 `2`로 사용자를 선택합니다. 이는 로그인 없이 권한 분기를 체험하기 위한 데모 인터페이스이며 인증 수단이 아닙니다. 실제 사용자 인증은 이 간단한 프로젝트의 범위에 포함하지 않습니다.

## API
| Method | Path | 동작 |
|---|---|---|
| GET | /api/users | 데모 사용자 |
| GET / POST | /api/posts | 목록(페이지), 글 작성 |
| GET / PUT / DELETE | /api/posts/{id} | 조회, 수정, 삭제 |
| GET / POST | /api/posts/{id}/comments | 댓글 목록, 작성 |
| PUT / DELETE | /api/posts/{id}/comments/{commentId} | 댓글 수정, 삭제 |
| PUT / DELETE | /api/posts/{id}/likes/me | 좋아요 등록, 취소 |

POST/PUT JSON: 글은 `title`, `body`(수정은 `version` 추가), 댓글은 `body`. 오류는 HTTP 상태와 ProblemDetail로 반환합니다.

## 소스 기반 시각화
`scripts/export-model.mjs`는 실제 `schema.sql`, JPA entity, controller, service, validation 선언을 읽고 FK 관계·API·흐름·소스 위치와 SHA-256을 추출합니다. 추출 결과와 현재 Git commit으로 포트폴리오 프로젝트 상세를 구성합니다.

## 멀티파트 데이터 + 이미지
새 글에 PNG/JPEG 이미지 한 장을 첨부할 수 있습니다 (2MB, 4096×4096 이하).
POST /api/posts (multipart/form-data)의 data 파트는 application/json {title, body}, image 파트는 원본 파일 바이너리입니다. FormData 사용 시 Content-Type 헤더를 직접 지정하지 않습니다.
이미지는 post_images BLOB에 저장되며 게시글과 한 트랜잭션으로 처리됩니다. 응답 images의 url을 GET /api/images/{id}로 조회합니다. 글 삭제 시 이미지도 먼저 삭제합니다. 파일 형식과 실제 이미지 내용이 일치해야 합니다.
기존 JSON 글 작성 API도 유지합니다. 브라우저 체험과 달리 이 서버의 이미지 데이터는 H2에 저장됩니다.
