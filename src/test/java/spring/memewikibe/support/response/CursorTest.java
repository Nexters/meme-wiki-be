package spring.memewikibe.support.response;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Description;
import org.springframework.test.util.ReflectionTestUtils;
import spring.memewikibe.domain.BaseEntity;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

class CursorTest {

    private static FixtureEntity createFixtureEntity(Long id) {
        FixtureEntity entity = new FixtureEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    @Test
    void 커서형_크기_제한이_0보다_작을_수_없다() {
        thenThrownBy(() -> Cursor.of(List.of(), -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("크기 제한은 0보다 커야 합니다.");
    }

    @Test
    void 목록이_크기_제한보다_크면_다음항목이_존재한다() {
        Cursor cursor = Cursor.of(List.of(createFixtureEntity(1L), createFixtureEntity(2L)), 1);

        then(cursor.getNext()).isEqualTo(1L);
        then(cursor.isHasMore()).isTrue();
        then(cursor.getPageSize()).isEqualTo(1);
    }

    @Test
    void 목록이_크지_제한보다_크지않으면_다음항목이_존재하지않는다() {
        Cursor cursor = Cursor.of(List.of(createFixtureEntity(1L), createFixtureEntity(2L)), 3);

        then(cursor.getNext()).isNull();
        then(cursor.isHasMore()).isFalse();
        then(cursor.getPageSize()).isEqualTo(2);
    }

    @Description("엔티티 목록 비어있을 때 빈 객체를 반환할 필요가 없을 수 있다.")
    @Test
    void 엔티티목록이_비어있으면_빈_커서객체를_반환한다() {
        Cursor cursor = Cursor.of(List.of(), 10);

        then(cursor.getNext()).isNull();
        then(cursor.isHasMore()).isFalse();
        then(cursor.getPageSize()).isEqualTo(0);
    }

    @Test
    void next값은_리스트의_크기와_limit의_크기중_작은_것의_id가_next값이다() {
        Cursor cursor = Cursor.of(List.of(
            createFixtureEntity(1L),
            createFixtureEntity(2L),
            createFixtureEntity(3L)
        ), 2);

        then(cursor.getNext()).isEqualTo(2L);
        then(cursor.isHasMore()).isTrue();
        then(cursor.getPageSize()).isEqualTo(2);
    }

    @Entity
    static class FixtureEntity extends BaseEntity {

    }
}