package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DumpServiceImpl implements DumpService {

    private final DumpRepository dumpRepository;
    private final DumpFetcher dumpFetcher;
    private boolean updateChecked = false;

    @Override
    public DiscogsDump getDumpByEtag(String etag) throws DumpNotFoundException {
        updateDumps();
        etag = etag.replace("\"", "");
        if (!dumpRepository.existsByEtag(etag)) {
            throw new DumpNotFoundException(etag);
        }
        return dumpRepository.findByEtag(etag);
    }

    @Override
    public DiscogsDump getMostRecentDumpByType(DumpType type) {
        updateDumps();
        return dumpRepository.findTopByDumpTypeOrderByIdDesc(type);
    }

    @Override
    public List<DiscogsDump> getLatestCompletedDumpSet() {
        updateDumps();

        LocalDateTime current = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        LocalDateTime start = LocalDateTime.of(current.toLocalDate(), current.toLocalTime());

        List<DiscogsDump> dumps = new ArrayList<>();

        while (dumps.size() < 4) {
            dumps = dumpRepository.findAllByLastModifiedIsBetween(start, start.plusMonths(1));
            start = start.minusMonths(1);
        }

        return dumps;
    }

    @Override
    public List<DiscogsDump> getDumpListInRange(LocalDateTime start, LocalDateTime end) {
        return dumpRepository
                .findAllByLastModifiedIsBetween(start, end);
    }

    @Override
    public DiscogsDump getDumpByDumpTypeInRange(DumpType dumpType, LocalDateTime from, LocalDateTime to) {
        return dumpRepository.findByDumpTypeAndLastModifiedIsBetween(dumpType, from, to);
    }

    @Override
    public List<DiscogsDump> getDumpListInYearMonth(int year, int month) {
        LocalDateTime start = getYearMonthInitialDateTime(year, month);
        return dumpRepository.findAllByLastModifiedIsBetween(start, start.plusMonths(1));
    }

    @Override
    public void updateDumps() {
        if (updateChecked) return;

        updateChecked = true;

        OffsetDateTime current = OffsetDateTime.now(ZoneId.of("UTC"));
        int month = current.getMonthValue();
        int year = current.getYear();

        LocalDateTime start = LocalDateTime.of(LocalDate.of(year, month, 1), LocalTime.MIN);
        LocalDateTime end = start.plusMonths(1);

        int count = dumpRepository.countByLastModifiedBetween(start, end);

        if (count == 4) {
            log.debug("full monthly dump found for {}-{}", year, month);
            return;
        }

        List<DiscogsDump> target = dumpFetcher.getDiscogsDumps();
        long repoSize = dumpRepository.count();

        if (repoSize == target.size()) {
            log.debug("repository up-to-date. skip update");
            return;
        }

        if (repoSize < target.size()) {
            log.debug("begin update >> current {} target {}", repoSize, target.size());
        }

        target = target.stream()
                .filter(item -> !dumpRepository.existsByEtag(item.getEtag()))
                .collect(Collectors.toList());

        dumpRepository.saveAll(target);
    }

    @Override
    public List<DiscogsDump> getAllDumps() {
        updateDumps();
        return dumpRepository.findAll();
    }

    @Override
    public boolean isExistsByEtag(String etag) {
        updateDumps();
        return dumpRepository.existsByEtag(etag);
    }

    private OffsetDateTime getCurrentYearMonthDateTime() {
        return OffsetDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .withOffsetSameInstant(ZoneOffset.UTC);
    }

    private LocalDateTime getYearMonthInitialDateTime(int year, int month) {
        return LocalDateTime.now()
                .withYear(year)
                .withMonth(month)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}
