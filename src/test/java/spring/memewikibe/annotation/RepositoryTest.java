package spring.memewikibe.annotation;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import spring.memewikibe.infrastructure.MemeAggregationRepositoryImpl;
import spring.memewikibe.infrastructure.QueryDslConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@Tag("repository")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Import({QueryDslConfig.class, MemeAggregationRepositoryImpl.class})
public @interface RepositoryTest {
}
