package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.release.ReleaseFormat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseFormatRepository extends JpaRepository<ReleaseFormat, Long> {
}
