package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DumpService {
    void updateDumps();

    DiscogsDump getDumpByEtag(String etag) throws DumpNotFoundException;

    DiscogsDump getMostRecentDumpByType(DumpType type);

    List<DiscogsDump> getDumpListInRange(LocalDate from, LocalDate to);

    List<DiscogsDump> getDumpListInRange(DiscogsDump dump);

    List<DiscogsDump> getLatestCompletedDumpSet();

    List<DiscogsDump> getAllDumps();

    boolean isExistsByEtag(String etag);

    boolean isUpdatedToday();

    Page<?> getAllDumpsByPage(Pageable pageable);
}
