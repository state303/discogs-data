package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class MasterGenre extends BaseTimeEntity {

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasterGenreId implements Serializable {
        @Column(name = "master_id")
        private Long masterId;
        @Column(name = "genre_id")
        private Long genreId;
    }

    @EmbeddedId
    private MasterGenreId masterGenreId;

    @JoinColumn(name = "master_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Master.class, fetch = FetchType.EAGER)
    private Master master;

    @JoinColumn(name = "genre_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Genre.class, fetch = FetchType.EAGER)
    private Genre genre;
}