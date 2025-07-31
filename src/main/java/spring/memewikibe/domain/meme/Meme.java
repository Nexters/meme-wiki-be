package spring.memewikibe.domain.meme;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.memewikibe.domain.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Meme extends BaseEntity {
    private String title;

    private String origin;

    private String usageContext;

    private String trendPeriod;

    private String imgUrl;

    private String hashtags;
}
