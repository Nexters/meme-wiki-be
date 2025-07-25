package spring.memewikibe.domain.meme;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import spring.memewikibe.domain.BaseEntity;

@Entity
public class MemeShareLog extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "meme_id", nullable = false)
    private Meme meme;

    protected MemeShareLog() {
    }
}
