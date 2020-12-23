package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDumpDependencyResolver implements DumpDependencyResolver {

    private final DumpService dumpService;

    @Override
    public List<DiscogsDump> resolveByEtag(Collection<String> etag) {
        Map<DumpType, DiscogsDump> dumpMap = getDumpMap(etag);
        dumpMap = resolveDumpDependency(dumpMap);
        return new ArrayList<>(dumpMap.values());
    }

    @Override
    public List<DiscogsDump> resolveByType(Collection<String> types) {
        Map<DumpType, DiscogsDump> dumpMap = types.stream()
                .map(type -> DumpType.valueOf(type.toUpperCase(Locale.ROOT)))
                .map(dumpService::getMostRecentDumpByType)
                .collect(Collectors.toMap(DiscogsDump::getDumpType, dump -> dump));
        return new ArrayList<>(resolveDumpDependency(dumpMap).values());
    }

    @Override
    public List<DiscogsDump> resolveByYearMonth(String yearMonth) {
        OffsetDateTime targetDateTime = parseOffsetDateTimeByYearMonth(yearMonth);
        return new ArrayList<>(dumpService.getDumpListInRange(targetDateTime, targetDateTime.plusMonths(1)));
    }

    @Override
    public List<DiscogsDump> resolveByTypeAndYearMonth(Collection<String> types, String yearMonth) {
        OffsetDateTime targetDateTime = parseOffsetDateTimeByYearMonth(yearMonth);

        List<DumpType> typeList = types.stream()
                .map(String::toUpperCase)
                .map(DumpType::valueOf)
                .collect(Collectors.toList());

        Map<DumpType, DiscogsDump> dumpMap = new HashMap<>();

        typeList.forEach(type -> {
            DiscogsDump dump =
                    dumpService.getDumpByDumpTypeInRange(type, targetDateTime, targetDateTime.plusMonths(1));
            dumpMap.put(dump.getDumpType(), dump);
        });

        return new ArrayList<>(resolveDumpDependency(dumpMap).values());
    }

    private Map<DumpType, DiscogsDump> getDumpMap(Collection<String> etags) {
        Map<DumpType, DiscogsDump> dumpMap = new HashMap<>();
        for (String etag : etags) {
            if (dumpService.isExistsByEtag(etag)) {
                DiscogsDump dump = dumpService.getDumpByEtag(etag);
                if (dumpMap.containsKey(dump.getDumpType())) {
                    DiscogsDump previous = dumpMap.get(dump.getDumpType());
                    if (previous.getLastModified().isBefore(dump.getLastModified())) {
                        dumpMap.put(dump.getDumpType(), dump);
                        continue;
                    }
                }
                dumpMap.put(dump.getDumpType(), dump);
            }
        }
        return dumpMap;
    }

    private OffsetDateTime parseOffsetDateTimeByYearMonth(String yearMonth) {
        List<Integer> nums = Arrays.stream(yearMonth.split("-"))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        LocalDate localDate = LocalDate.of(nums.get(0), nums.get(1), 1);
        return OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC);
    }

    private Map<DumpType, DiscogsDump> resolveDumpDependency(Map<DumpType, DiscogsDump> dumpMap) {
        Map<DumpType, DiscogsDump> resultMap = new HashMap<>();
        dumpMap.forEach(resultMap::put);

        if (resultMap.containsKey(DumpType.RELEASE)) {
            return getDumpTypeDiscogsDumpMap(DumpType.RELEASE, resultMap);
        }

        if (resultMap.containsKey(DumpType.MASTER)) {
            return getDumpTypeDiscogsDumpMap(DumpType.MASTER, resultMap);
        }

        log.debug("no dependencies found. proceed...");
        return resultMap;
    }

    private Map<DumpType, DiscogsDump> getDumpTypeDiscogsDumpMap(DumpType dumpType, Map<DumpType, DiscogsDump> dumpMap) {
        List<DumpType> dependantTypes = getDependantTypes(dumpType);
        DiscogsDump referenceDump = dumpMap.get(dumpType);

        OffsetDateTime targetDateTime = referenceDump.getLastModified().withOffsetSameInstant(ZoneOffset.UTC);
        log.debug("resolving dependencies. fetching relevant dumps from: " + targetDateTime.getMonth() + ", " + targetDateTime.getYear());
        List<DiscogsDump> list = dumpService.getDumpListInYearMonth(targetDateTime.getYear(), targetDateTime.getMonthValue());
        list.stream()
                .filter(item -> dependantTypes.contains(item.getDumpType()))
                .forEach(replaceOlderDumps(dumpMap));
        return dumpMap;
    }

    private List<DumpType> getDependantTypes(DumpType type) {
        List<DumpType> types = new ArrayList<>(Arrays.asList(DumpType.LABEL, DumpType.ARTIST));
        switch (type) {
            case RELEASE: {
                types.add(DumpType.MASTER);
                return types;
            }
            case MASTER:
                return types;
            default:
                return new ArrayList<>();
        }
    }

    private Consumer<DiscogsDump> replaceOlderDumps(Map<DumpType, DiscogsDump> resultMap) {
        return item -> {
            if (resultMap.containsKey(item.getDumpType())) {
                DiscogsDump preListedDump = resultMap.get(item.getDumpType());
                if (item.getLastModified().isAfter(preListedDump.getLastModified())) {
                    log.debug("replacing dump {{}} to {{}}", preListedDump.getEtag(), item.getEtag());
                    resultMap.put(item.getDumpType(), item);
                }
            }
        };
    }
}
