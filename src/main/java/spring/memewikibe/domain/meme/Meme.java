package spring.memewikibe.domain.meme;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.memewikibe.domain.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Meme extends BaseEntity {
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String imgUrl;

    private String hashtags;

    @Getter
    public enum Category {
        NONE
    }
}
