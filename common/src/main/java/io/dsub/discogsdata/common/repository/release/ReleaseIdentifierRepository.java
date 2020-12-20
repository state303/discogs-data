package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.release.ReleaseIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseIdentifierRepository extends JpaRepository<ReleaseIdentifier, ReleaseIdentifier.ReleaseIdentifierId> {
}
