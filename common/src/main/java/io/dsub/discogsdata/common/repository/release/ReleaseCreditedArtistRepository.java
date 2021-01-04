package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.release.ReleaseCreditedArtist;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseCreditedArtistRepository extends JpaRepository<ReleaseCreditedArtist, Long> {
    boolean existsByArtistAndReleaseItem(Artist artist, ReleaseItem releaseItem);
}
