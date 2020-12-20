package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.release.ReleaseWork;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseWorkRepository extends JpaRepository<ReleaseWork, ReleaseWork.ReleaseWorksId> {
}
