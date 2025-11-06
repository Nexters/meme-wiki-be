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

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class MemeShareLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meme_id", nullable = false)
    private Meme meme;

    @Builder
    private MemeShareLog(Meme meme) {
        this.meme = meme;
    }

    public static MemeShareLog of(Meme meme) {
        Objects.requireNonNull(meme, "meme must not be null");
        return MemeShareLog.builder()
            .meme(meme)
            .build();
    }
}
