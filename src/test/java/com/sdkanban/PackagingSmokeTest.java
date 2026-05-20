package com.sdkanban;

import com.sdkanban.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc(addFilters = false)
class PackagingSmokeTest {
    private static final Path STATIC_DIR = createStaticDirectory();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @DynamicPropertySource
    static void staticResources(DynamicPropertyRegistry registry) {
        registry.add("spring.web.resources.static-locations", () -> STATIC_DIR.toUri().toString());
    }

    @Test
    void staticResourceHandlerServesFrontendIndexHtml() throws Exception {
        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<div id=\"app\"></div>")));
    }

    private static Path createStaticDirectory() {
        try {
            Path directory = Files.createTempDirectory("sd-kanban-static");
            Files.writeString(directory.resolve("index.html"), "<!doctype html><div id=\"app\"></div>");
            return directory;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create static resource fixture", exception);
        }
    }
}
