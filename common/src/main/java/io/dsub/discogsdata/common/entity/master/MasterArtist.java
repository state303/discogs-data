package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
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
public class MasterArtist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "master_id")
    @ManyToOne
    private Master master;

    @JoinColumn(name = "artist_id")
    @ManyToOne
    private Artist artist;
}
