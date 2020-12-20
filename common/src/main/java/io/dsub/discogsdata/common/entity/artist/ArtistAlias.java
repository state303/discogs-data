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
public class ArtistAlias extends BaseTimeEntity {
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtistAliasId implements Serializable {
        @Column(name = "artist_id")
        private Long artistId;
        @Column(name = "alias_id")
        private Long alias_id;
    }

    @EmbeddedId
    private ArtistAliasId artistAliasId;

    /*
     * Convenient READ_ONLY access for actually mapped class.
     * NOTE: mark any FetchType to avoid warning about immutability.
     */
    @JoinColumn(name = "artist_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Artist.class, fetch = FetchType.EAGER)
    private Artist artist;

    @JoinColumn(name = "alias_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Artist.class, fetch = FetchType.EAGER)
    private Artist alias;
}
