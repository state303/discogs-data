package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DumpServiceImpl implements DumpService {

    private final DumpRepository dumpRepository;
    private final SimpleDumpFetcher discogsDumpFetcher;

    private ConcurrentMap<DumpType, ConcurrentSkipListSet<DiscogsDump>> cache;
    private LocalDateTime lastUpdated = null;

    @Override
    public DiscogsDump getDumpByEtag(String etag) throws DumpNotFoundException {
        updateDumps();
        etag = etag.replace("\"", "");
        Optional<DiscogsDump> optionalDiscogsDump =
                dumpRepository.findOne(Example.of(DiscogsDump.builder().etag(etag).build()));
        String finalEtag = etag;
        return optionalDiscogsDump.orElseThrow(() -> new DumpNotFoundException(finalEtag));
    }

    @Override
    public DiscogsDump getMostRecentDumpByType(DumpType type) {
        updateDumps();
        return cache.get(type).last();
    }

    @Override
    public List<DiscogsDump> getLatestCompletedDumpSet() {
        updateDumps();

        LocalDateTime begin = LocalDateTime.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), 1, 0, 0);

        List<DiscogsDump> dumps = new ArrayList<>();

        while (dumps.size() < 4) {
            dumps = dumpRepository.findAllByLastModifiedIsBetween(begin, begin.plusMonths(1).minusDays(1));
            begin = begin.minusMonths(1);
        }

        return dumps;
    }

    @Override
    public List<DiscogsDump> getDumpListInRange(LocalDate start, LocalDate end) {
        LocalDateTime from = LocalDateTime.of(start.getYear(), start.getMonth(), 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(end, LocalTime.of(0, 0));
        return dumpRepository
                .findAllByLastModifiedIsBetween(from, to);
    }

    @Override
    public List<DiscogsDump> getDumpListInRange(DiscogsDump dump) {
        LocalDate start = dump.getLastModified().toLocalDate();
        LocalDate to = start.plusMonths(1).minusDays(1);
        return getDumpListInRange(start, to);
    }

    @Override
    public void updateDumps() {
        if (isUpdatedToday()) {
            return;
        }
        this.cache = createNewCache();
        lastUpdated = LocalDateTime.now();
    }

    public List<DiscogsDump> updateRepository() {
        String s3BucketUrl = discogsDumpFetcher.getS3BucketUrl();
        List<DiscogsDump> fullDump = discogsDumpFetcher.getDiscogsDumps(s3BucketUrl);
        List<DiscogsDump> targetDumpList = new ArrayList<>();

        long repoSize = dumpRepository.count();

        if (repoSize < fullDump.size() - 4) {
            log.debug(String.format("begin full update >> current %s target %s", repoSize, fullDump.size()));
            targetDumpList = fullDump;
        } else if (repoSize == fullDump.size()) {
            log.debug("repository up-to-date. skip update");
        } else {
            log.debug(String.format("begin partial update >> current %s target %s", repoSize, fullDump.size()));
            Collection<DiscogsDump> recentDumps = fullDump.stream()
                    .collect(Collectors.toMap(
                            (DiscogsDump::getDumpType),
                            (dump -> dump),
                            ((prev, curr) ->
                                    prev.getLastModified()
                                            .isAfter(curr.getLastModified()) ? prev : curr)))
                    .values();
            targetDumpList.addAll(recentDumps);
        }

        dumpRepository.saveAll(targetDumpList);
        return fullDump;
    }

    @Override
    public List<DiscogsDump> getAllDumps() {
        updateDumps();
        List<DiscogsDump> sum = new ArrayList<>();
        this.cache.forEach((dumpType, discogsDumps) -> sum.addAll(discogsDumps));
        return sum;
    }

    @Override
    public boolean isExistsByEtag(String etag) {
        updateDumps();
        return dumpRepository.existsByEtag(etag);
    }

    @Override
    public Page<?> getAllDumpsByPage(Pageable pageable) {
        updateDumps();
        return dumpRepository.findAll(pageable).map(DiscogsDump::toDto);
    }

    @Override
    public boolean isUpdatedToday() {
        if (lastUpdated == null) return false;
        return lastUpdated.toLocalDate()
                .isEqual(LocalDate.now());
    }

    private ConcurrentMap<DumpType, ConcurrentSkipListSet<DiscogsDump>> createNewCache() {
        List<DiscogsDump> fullDump = updateRepository();

        Function<DumpType, ConcurrentSkipListSet<DiscogsDump>> dumpSetByType =
                dumpType -> fullDump.stream()
                        .filter(item -> item.getDumpType().equals(dumpType))
                        .collect(Collectors.toCollection(ConcurrentSkipListSet::new));

        return Arrays.stream(DumpType.values())
                .collect(Collectors.toConcurrentMap((item -> item), (dumpSetByType)));
    }
}
