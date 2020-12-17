package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.release.ReleaseCreditedArtist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseCreditedArtistRepository extends JpaRepository<ReleaseCreditedArtist, Long> {
}
