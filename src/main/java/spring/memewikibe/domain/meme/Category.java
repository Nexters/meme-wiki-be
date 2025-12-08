package spring.memewikibe.domain.meme;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.memewikibe.domain.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Category extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private String imgUrl;

    @Builder
    private Category(String name, String imgUrl) {
        this.name = name;
        this.imgUrl = imgUrl;
    }

    /**
     * Category 생성을 위한 정적 팩토리 메서드
     */
    public static Category create(String name, String imgUrl) {
        return Category.builder()
                .name(name)
                .imgUrl(imgUrl)
                .build();
    }
}
