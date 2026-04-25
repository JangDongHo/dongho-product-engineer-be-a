package io.github.jangdongho.productengineer.persistence.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA가 활성일 때만 Auditing을 켠다. {@code @WebMvcTest} 등 JPA 슬라이스에서는
 * {@link EntityManagerFactory}가 없으므로 컨텍스트가 뜨지 않는 문제를 피한다.
 */
@Configuration
@ConditionalOnBean(EntityManagerFactory.class)
@EnableJpaAuditing
public class JpaAuditingConfig {
}
