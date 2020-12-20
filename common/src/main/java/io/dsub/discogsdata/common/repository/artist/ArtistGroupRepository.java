package io.dsub.discogsdata.common.repository.artist;

import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistGroupRepository extends JpaRepository<ArtistGroup, ArtistGroup.ArtistGroupId> {

}