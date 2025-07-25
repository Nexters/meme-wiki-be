package spring.memewikibe.domain.meme;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import spring.memewikibe.domain.BaseEntity;

@Entity
public class Hashtag extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    protected Hashtag() {
    }
}
