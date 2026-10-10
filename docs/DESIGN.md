# 소스 해석과 기능 검증

## 해석의 기준
1. `schema.sql`의 CREATE TABLE, FOREIGN KEY, UNIQUE를 읽어 4개 테이블과 5개 FK를 추출합니다.
2. JPA `@Table`과 대조하여 모든 테이블이 entity로 매핑되는지 확인합니다.
3. `BoardController`의 mapping과 validation을 읽어 실제 API 및 길이 제한을 추출합니다.
4. `BoardService`의 메서드·작성자 검사·버전 검사·삭제 순서·좋아요 조건을 읽고 동작 단계를 구성합니다.

흐름 이름과 설명은 코드의 해석입니다. DB 관계, 제약, API, 검증 제한은 실제 선언에서 추출합니다. 각 해석은 소스 파일·줄 번호·검증한 코드 조각을 가집니다. 일반 Java 분석기가 아니라 이 저장소의 명시적인 모델 추출기이며 알 수 없는 DDL과 근거 누락은 실패합니다.

```sh
node scripts/export-model.mjs . docs/schema-model.json
```

## 요구사항과 종료 조건
| 요구사항 | 종료 조건 |
|---|---|
| 글 작성·수정·삭제 | 실제 API 요청과 DB 결과 검증 |
| 댓글 작성·수정·삭제 | 작성자와 소속 글 검사, 삭제 검증 |
| 좋아요 | 사용자별 1건, 중복 PUT/DELETE 멱등성 |
| 관계 표시 | 4 tables, 5 FKs, unique(post_id,user_id) 소스 일치 |
| GitHub 게시 | source push, main SHA 확인, Actions test |
| 포트폴리오 UI | 프로젝트 목록 → 선택 → 상세 설계·게시판 체험 |

API 통합 테스트는 작성·버전 변경·댓글·좋아요·FK를 유지하는 연관 삭제, 작성자 권한, 댓글 소속, 빈값/길이/404/409, 페이지를 검증합니다.

포트폴리오의 공개 체험은 브라우저 내 시뮬레이션입니다. 저장소의 Spring Boot UI는 실제 API 및 H2 DB에 연결됩니다. 포트폴리오가 Java 서버에서 실행된다는 뜻이 아닙니다.
