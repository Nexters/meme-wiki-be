package spring.memewikibe.api.controller.meme.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 밈 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class MemeUpdateRequest {
    
    @NotBlank(message = "제목은 필수입니다.")
    private String title;
    
    @NotBlank(message = "출처는 필수입니다.")
    private String origin;
    
    @NotBlank(message = "사용맥락은 필수입니다.")
    private String usageContext;
    
    @NotBlank(message = "유행시기는 필수입니다.")
    private String trendPeriod;
    
    private String hashtags;
    
    private String imgUrl;
    
    private List<Long> categoryIds;
    
    public MemeUpdateRequest(String title, String origin, String usageContext, 
                           String trendPeriod, String hashtags, String imgUrl, List<Long> categoryIds) {
        this.title = title;
        this.origin = origin;
        this.usageContext = usageContext;
        this.trendPeriod = trendPeriod;
        this.hashtags = hashtags;
        this.imgUrl = imgUrl;
        this.categoryIds = categoryIds;
    }
}
