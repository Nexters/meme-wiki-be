package spring.memewikibe.domain.meme;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    private String origin;

    private String usageContext;

    private String trendPeriod;

    private String imgUrl;

    private String hashtags;

    @Enumerated(EnumType.STRING)
    private Flag flag;

    @Builder
    private Meme(String title, String origin, String usageContext, String trendPeriod, String imgUrl, String hashtags, Flag flag) {
        this.title = title;
        this.origin = origin;
        this.usageContext = usageContext;
        this.trendPeriod = trendPeriod;
        this.imgUrl = imgUrl;
        this.hashtags = hashtags;
        this.flag = flag;
    }

    public static Meme crawlerMeme(String title, String origin, String usageContext, String trendPeriod, String imgUrl, String hashtags) {
        return Meme.builder()
            .title(title)
            .origin(origin)
            .usageContext(usageContext)
            .trendPeriod(trendPeriod)
            .imgUrl(imgUrl)
            .hashtags(hashtags)
            .flag(Flag.ABNORMAL)
            .build();
    }

    public enum Flag {
        NORMAL("정상"),
        ABNORMAL("비정상");

        private final String description;

        Flag(String description) {
            this.description = description;
        }
    }
    
    /**
     * 밈 정보를 업데이트합니다.
     */
    public void updateMeme(String title, String origin, String usageContext, 
                          String trendPeriod, String imgUrl, String hashtags) {
        this.title = title;
        this.origin = origin;
        this.usageContext = usageContext;
        this.trendPeriod = trendPeriod;
        this.imgUrl = imgUrl;
        this.hashtags = hashtags;
    }
    
    /**
     * 밈을 정상 상태로 승인합니다.
     */
    public void approve() {
        this.flag = Flag.NORMAL;
    }
    
    /**
     * 밈을 비정상 상태로 되돌립니다.
     */
    public void reject() {
        this.flag = Flag.ABNORMAL;
    }
}
