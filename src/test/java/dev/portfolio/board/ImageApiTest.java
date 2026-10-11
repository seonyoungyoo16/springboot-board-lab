package dev.portfolio.board;

import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:image-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ImageApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PostRepository posts;
    @Autowired ImageRepository images;
    private MockMultipartFile data(String title) { return new MockMultipartFile("data", "data.json", "application/json", ("{\"title\":\"" + title + "\",\"body\":\"본문\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private byte[] png() throws Exception { var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out); return out.toByteArray(); }
    @Test void uploadRetrieveAndDelete() throws Exception {
        byte[] bytes = png();
        var response = mvc.perform(multipart("/api/posts").file(data("이미지 글")).file(new MockMultipartFile("image", "../사진.png", "image/png", bytes))).andExpect(status().isCreated()).andExpect(jsonPath("$.images[0].filename").value("사진.png")).andReturn().getResponse();
        var post = json.readTree(response.getContentAsString()); long id = post.get("id").asLong(); String url = post.get("images").get(0).get("url").asText();
        mvc.perform(get("/api/posts/" + id)).andExpect(jsonPath("$.images.length()").value(1));
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(bytes)).andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(delete("/api/posts/" + id).header("X-Demo-User", "2")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/posts/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }
    @Test void invalidUploadsLeaveNoPostOrImage() throws Exception {
        long count = posts.count(), imageCount = images.count();
        mvc.perform(multipart("/api/posts").file(data(" ")).file(new MockMultipartFile("image", "a.png", "image/png", png()))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/posts").file(data("제목"))).andExpect(status().isBadRequest());
        for (var image : new MockMultipartFile[]{new MockMultipartFile("image", "a.png", "image/png", new byte[0]), new MockMultipartFile("image", "a.png", "image/png", "fake".getBytes()), new MockMultipartFile("image", "a.svg", "image/svg+xml", png()), new MockMultipartFile("image", "a.jpg", "image/jpeg", png()), new MockMultipartFile("image", "a.png", "image/png", new byte[2097153])}) {
            mvc.perform(multipart("/api/posts").file(data("제목")).file(image)).andExpect(status().isBadRequest());
        }
        assertThat(posts.count()).isEqualTo(count); assertThat(images.count()).isEqualTo(imageCount);
    }
}
