package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.label.Label;
import io.dsub.discogsdata.common.entity.label.LabelSubLabel;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class MasterArtist extends BaseTimeEntity {

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasterArtistId implements Serializable {
        @Column(name = "master_id")
        private Long masterId;
        @Column(name = "artist_id")
        private Long artistId;
    }

    @EmbeddedId
    private MasterArtistId masterArtistId;

    @JoinColumn(name = "master_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Master.class, fetch = FetchType.EAGER)
    private Master master;

    @JoinColumn(name = "artist_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Artist.class, fetch = FetchType.EAGER)
    private Artist artist;
}
