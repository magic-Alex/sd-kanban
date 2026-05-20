package com.sdkanban.project.repository;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ProjectPersistenceAvailableCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String exclusions = context.getEnvironment().getProperty("spring.autoconfigure.exclude", "");
        return !exclusions.contains("DataSourceAutoConfiguration")
            && !exclusions.contains("HibernateJpaAutoConfiguration");
    }
}
