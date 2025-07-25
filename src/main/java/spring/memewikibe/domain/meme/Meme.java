package spring.memewikibe.domain.meme;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import spring.memewikibe.domain.BaseEntity;

@Entity
public class Meme extends BaseEntity {
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String imgUrl;

    protected Meme() {
    }

    public enum Category {
        NONE
    }
}
