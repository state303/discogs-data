package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.exception.DumpNotFoundException;

import java.time.OffsetDateTime;
import java.util.List;

public interface DumpService {
    void updateDumps();

    DiscogsDump getDumpByEtag(String etag) throws DumpNotFoundException;

    DiscogsDump getMostRecentDumpByType(DumpType type);

    List<DiscogsDump> getDumpListInRange(OffsetDateTime from, OffsetDateTime to);

    DiscogsDump getDumpByDumpTypeInRange(DumpType dumpType, OffsetDateTime from, OffsetDateTime to);

    List<DiscogsDump> getDumpListInYearMonth(int year, int month);

    List<DiscogsDump> getLatestCompletedDumpSet();

    List<DiscogsDump> getAllDumps();

    boolean isExistsByEtag(String etag);
}
