package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface DumpRepository extends JpaRepository<DiscogsDump, Long> {
    boolean existsByEtag(String etag);
    DiscogsDump findTopByDumpTypeOrderByIdDesc(DumpType dumpType);
    List<DiscogsDump> findAllByDumpTypeOrderByLastModifiedDesc(DumpType dumpType);
    List<DiscogsDump> findAllByLastModifiedIsBetween(LocalDateTime begin, LocalDateTime end);
    DiscogsDump findByDumpTypeAndLastModifiedIsBetween(DumpType dumpType, LocalDateTime begin, LocalDateTime end);
    DiscogsDump findByEtag(String etag);
    DiscogsDump findTopByOrderByLastModifiedDesc();
    int countByLastModifiedBetween(LocalDateTime begin, LocalDateTime end);
}