package io.dsub.discogsdata.common.repository.release;

import io.dsub.discogsdata.common.entity.release.ReleaseVideo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseVideoRepository extends JpaRepository<ReleaseVideo, ReleaseVideo.ReleaseVideoId> {
}
