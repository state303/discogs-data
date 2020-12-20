package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.release.ReleaseArtist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseArtistRepository extends JpaRepository<ReleaseArtist, ReleaseArtist.ReleaseArtistId> {
}
