package dev.portfolio.board;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:board-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class BoardApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CommentRepository comments;
    @Autowired LikeRepository likes;
    private JsonNode create() throws Exception { return json.readTree(mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"설계 기록\",\"body\":\"테이블 관계\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()); }
    @Test void completeLifecycleAndCascade() throws Exception {
        var p = create(); long id = p.get("id").asLong();
        mvc.perform(put("/api/posts/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"수정 제목\",\"body\":\"수정 본문\",\"version\":0}")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        var response = mvc.perform(post("/api/posts/" + id + "/comments").header("X-Demo-User", "2").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"댓글\"}")).andExpect(status().isCreated()).andReturn().getResponse();
        long cid = json.readTree(response.getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/posts/" + id + "/comments/" + cid).header("X-Demo-User", "2").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"수정 댓글\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.body").value("수정 댓글"));
        mvc.perform(put("/api/posts/" + id + "/likes/me").header("X-Demo-User", "2")).andExpect(status().isOk()).andExpect(jsonPath("$.likeCount").value(1));
        mvc.perform(get("/api/posts/" + id)).andExpect(jsonPath("$.commentCount").value(1));
        mvc.perform(delete("/api/posts/" + id)).andExpect(status().isNoContent());
        assertThat(comments.countByPostId(id)).isZero(); assertThat(likes.countByPostId(id)).isZero();
        mvc.perform(get("/api/posts/" + id)).andExpect(status().isNotFound());
    }
    @Test void likeIsIdempotentAndPerUser() throws Exception {
        long id = create().get("id").asLong();
        for (int i = 0; i < 2; i++) mvc.perform(put("/api/posts/" + id + "/likes/me")).andExpect(jsonPath("$.likeCount").value(1));
        mvc.perform(put("/api/posts/" + id + "/likes/me").header("X-Demo-User", "2")).andExpect(jsonPath("$.likeCount").value(2));
        for (int i = 0; i < 2; i++) mvc.perform(delete("/api/posts/" + id + "/likes/me")).andExpect(jsonPath("$.likeCount").value(1)).andExpect(jsonPath("$.liked").value(false));
    }
    @Test void ownershipAndCommentPostBoundary() throws Exception {
        long id = create().get("id").asLong(); long other = create().get("id").asLong();
        mvc.perform(delete("/api/posts/" + id).header("X-Demo-User", "2")).andExpect(status().isForbidden());
        mvc.perform(put("/api/posts/" + id).header("X-Demo-User", "2").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"수정\",\"body\":\"본문\",\"version\":0}")).andExpect(status().isForbidden());
        long cid = json.readTree(mvc.perform(post("/api/posts/" + id + "/comments").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"댓글\"}")).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete("/api/posts/" + id + "/comments/" + cid).header("X-Demo-User", "2")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/posts/" + other + "/comments/" + cid)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/posts/" + id + "/comments/" + cid)).andExpect(status().isNoContent());
    }
    @Test void validationMissingRowsAndStaleVersion() throws Exception {
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  \",\"body\":\"본문\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/posts").header("X-Demo-User", "999").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"제목\",\"body\":\"본문\"}")).andExpect(status().isNotFound());
        long id = create().get("id").asLong();
        mvc.perform(put("/api/posts/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"수정\",\"body\":\"본문\",\"version\":99}")).andExpect(status().isConflict());
        mvc.perform(post("/api/posts/" + id + "/comments").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/posts/999999/likes/me")).andExpect(status().isNotFound());
    }
    @Test void paginationAndLengthLimits() throws Exception {
        create();
        mvc.perform(get("/api/posts?page=0&size=1")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("title", "a".repeat(121), "body", "본문")))).andExpect(status().isBadRequest());
    }
}
