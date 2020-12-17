package io.dsub.discogsdata.common.repository;

import io.dsub.discogsdata.common.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
