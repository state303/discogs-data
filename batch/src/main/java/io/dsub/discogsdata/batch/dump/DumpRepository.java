package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DumpRepository extends JpaRepository<DiscogsDump, Long> {
    boolean existsByEtag(String etag);

    DiscogsDump findTopByDumpTypeOrderByIdDesc(DumpType dumpType);

    List<DiscogsDump> findAllByDumpTypeOrderByLastModifiedDesc(DumpType dumpType);

    List<DiscogsDump> findAllByLastModifiedIsBetween(LocalDateTime begin, LocalDateTime end);

    Page<DiscogsDump> findAll(Pageable pageable);
}