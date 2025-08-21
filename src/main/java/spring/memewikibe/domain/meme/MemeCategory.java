package spring.memewikibe.domain.meme;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.memewikibe.domain.BaseEntity;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class MemeCategory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meme_id", nullable = false)
    private Meme meme;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder
    private MemeCategory(Meme meme, Category category) {
        this.meme = meme;
        this.category = category;
    }
    
    /**
     * MemeCategory 생성을 위한 정적 팩토리 메서드
     */
    public static MemeCategory create(Meme meme, Category category) {
        return MemeCategory.builder()
                .meme(meme)
                .category(category)
                .build();
    }
}
