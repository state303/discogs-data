package io.dsub.discogsdata.common.entity.artist;

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
public class ArtistGroup extends BaseTimeEntity {

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtistGroupId implements Serializable {
        @Column(name = "artist_id")
        private Long artistId;
        @Column(name = "group_id")
        private Long groupId;
    }

    @EmbeddedId
    private ArtistGroupId artistGroupId;

    /*
     * Convenient READ_ONLY access for actually mapped class.
     * NOTE: mark any FetchType to avoid warning about immutability.
     */
    @JoinColumn(name = "artist_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Artist.class, fetch = FetchType.EAGER)
    private Artist artist;

    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Artist.class, fetch = FetchType.EAGER)
    private Artist group;
}
