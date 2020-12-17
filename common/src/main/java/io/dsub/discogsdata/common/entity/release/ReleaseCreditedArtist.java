package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.artist.Artist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseCreditedArtist {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "release_id")
    private ReleaseItem releaseItem;

    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;

}
