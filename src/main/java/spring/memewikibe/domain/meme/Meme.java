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
public class Meme extends BaseEntity {
    private String title;

    @Column(columnDefinition = "TEXT")
    private String origin;

    @Column(columnDefinition = "TEXT")
    private String usageContext;

    private String trendPeriod;

    private String imgUrl;

    private String hashtags;

    @Builder
    private Meme(String title, String origin, String usageContext, String trendPeriod, String imgUrl, String hashtags) {
        this.title = title;
        this.origin = origin;
        this.usageContext = usageContext;
        this.trendPeriod = trendPeriod;
        this.imgUrl = imgUrl;
        this.hashtags = hashtags;
    }

}
