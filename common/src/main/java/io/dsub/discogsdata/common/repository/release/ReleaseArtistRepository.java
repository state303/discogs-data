package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.release.ReleaseArtist;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseArtistRepository extends JpaRepository<ReleaseArtist, Long> {
    boolean existsByArtistAndReleaseItem(Artist artist, ReleaseItem releaseItem);
}
