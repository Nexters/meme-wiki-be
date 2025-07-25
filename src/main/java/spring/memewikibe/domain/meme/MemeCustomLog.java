package spring.memewikibe.domain.meme;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import spring.memewikibe.domain.BaseEntity;

@Entity
public class MemeCustomLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meme_id", nullable = false)
    private Meme meme;

    protected MemeCustomLog() {
    }
}
