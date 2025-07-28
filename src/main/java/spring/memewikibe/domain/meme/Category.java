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
}
