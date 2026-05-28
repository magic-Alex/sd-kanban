package com.sdkanban.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeConfigurationTest {
    @Test
    void applicationConfigurationExposesRuntimeEnvironmentOverrides() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
            .load("applicationConfig", new ClassPathResource("application.yml"));
        PropertySource<?> application = sources.get(0);

        assertThat(application.getProperty("server.port")).isEqualTo("${SERVER_PORT:8101}");
        assertThat(application.getProperty("spring.datasource.url")).isEqualTo("${DB_URL:jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:sd_kanban}?useUnicode=true&characterEncoding=utf8&serverTimezone=${DB_TIMEZONE:Asia/Shanghai}&createDatabaseIfNotExist=${DB_CREATE_DATABASE_IF_NOT_EXIST:true}}");
        assertThat(application.getProperty("spring.datasource.username")).isEqualTo("${DB_USERNAME:root}");
        assertThat(application.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD:root}");
        assertThat(application.getProperty("spring.flyway.enabled")).isEqualTo("${FLYWAY_ENABLED:true}");
        assertThat(application.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("${JPA_DDL_AUTO:validate}");
        assertThat(application.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET:sd-kanban-local-development-secret-change-before-production}");
        assertThat(application.getProperty("app.jwt.expires-minutes")).isEqualTo("${JWT_EXPIRES_MINUTES:720}");
        assertThat(application.getProperty("app.cors.allowed-origins")).isEqualTo("${CORS_ALLOWED_ORIGINS:http://localhost:8102}");
    }
}
