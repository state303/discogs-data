package io.dsub.discogsdata.common.repository.master;

import io.dsub.discogsdata.common.entity.master.MasterVideo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterVideoRepository extends JpaRepository<MasterVideo, MasterVideo.MasterVideoId> {
}
