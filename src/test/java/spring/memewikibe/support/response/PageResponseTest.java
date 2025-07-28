package spring.memewikibe.support.response;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import spring.memewikibe.domain.BaseEntity;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

class PageResponseTest {

    @Test
    void 페이지네이션_방식이_커서형이면_리스트의_가장_마지막요소를_제거하고_반환한다() {
        // given
        PageResponse<Cursor, String> pageResponse = PageResponse.cursor(
            Cursor.of(List.of(createFixtureEntity(1L), createFixtureEntity(2L), createFixtureEntity(3L)), 2),
            List.of("a", "b", "c")
        );

        // when
        List<String> result = pageResponse.getResults();

        // then
        then(result).hasSize(2);
        then(result).containsExactly("a", "b");
    }

    static class FixtureEntity extends BaseEntity {

    }

    private static FixtureEntity createFixtureEntity(Long id) {
        FixtureEntity entity = new FixtureEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}