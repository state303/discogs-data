package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.label.Label;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseArtist extends BaseTimeEntity {

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseArtistId implements Serializable {
        @Column(name = "release_item_id")
        private Long releaseItemId;
        @Column(name = "artist_id")
        private Long artistId;
    }

    @EmbeddedId
    private ReleaseArtistId releaseArtistId;

    @JoinColumn(name = "release_item_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = ReleaseItem.class, fetch = FetchType.EAGER)
    private ReleaseItem releaseItem;

    @JoinColumn(name = "artist_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Artist.class, fetch = FetchType.EAGER)
    private Artist artist;
}
