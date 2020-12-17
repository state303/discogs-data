package io.dsub.discogsdata.batch.process;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.common.exception.InvalidRequestParamException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JobExecutionResolver {

    private final DumpService dumpService;
    private final BiConsumer<Map<DumpType, DiscogsDump>, DiscogsDump> mergeRecent = (map, dump) -> {
        DumpType type = dump.getDumpType();
        if (map.containsKey(type)) {
            map.put(type, getRecent(dump, map.get(type)));
        } else {
            map.put(type, dump);
        }
    };

    public List<DiscogsDump> resolve(Collection<DiscogsDump> dumpList) {
        dumpList = resolveDuplicates(dumpList);
        dumpList = resolveDependencies(dumpList);
        return dumpList.stream()
                .sorted(Comparator.comparing(DiscogsDump::getDumpType))
                .collect(Collectors.toList());
    }

    private List<DiscogsDump> resolveDependencies(Collection<DiscogsDump> dumps) {

        Map<DumpType, DiscogsDump> requestMap = dumps.stream()
                .collect(Collectors.toMap(DiscogsDump::getDumpType, item -> item));

        if (requestMap.containsKey(DumpType.RELEASE)) {
            if (requestMap.size() == 4) {
                return new ArrayList<>(requestMap.values());
            }
            return dumpService.getDumpListInRange(requestMap.get(DumpType.RELEASE));
        } else if (requestMap.containsKey(DumpType.MASTER)) {
            return dumpService.getDumpListInRange(requestMap.get(DumpType.MASTER)).stream()
                    .filter(item -> !item.getDumpType().equals(DumpType.RELEASE))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(requestMap.values());
    }

    public Collection<DiscogsDump> resolveDuplicates(Collection<DiscogsDump> requestList) throws InvalidRequestParamException {
        if (requestList == null || requestList.isEmpty()) {
            throw new InvalidRequestParamException("Request is empty");
        }

        Map<DumpType, DiscogsDump> cache = new HashMap<>();
        requestList.forEach(dump -> mergeRecent.accept(cache, dump));
        return cache.values();
    }

    private DiscogsDump getRecent(DiscogsDump here, DiscogsDump other) {
        if (here.getLastModified().isBefore(other.getLastModified())) {
            return other;
        }
        return here;
    }

    public void afterPropertiesSet() {

    }
}
