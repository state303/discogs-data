package io.dsub.discogsdata.common.repository;

import io.dsub.discogsdata.common.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {
}
