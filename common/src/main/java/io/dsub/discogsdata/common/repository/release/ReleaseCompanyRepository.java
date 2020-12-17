package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.release.ReleaseCompany;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseCompanyRepository extends JpaRepository<ReleaseCompany, Long> {
}
