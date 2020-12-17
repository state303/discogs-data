package io.dsub.discogsdata.common.repository.artist;

import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistAliasRepository extends JpaRepository<ArtistAlias, Long> {
}
