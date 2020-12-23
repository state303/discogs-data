package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Local;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnMissingBean(value = DiscogsJobParameterFormatter.class)
@RequiredArgsConstructor
public class SimpleDiscogsJobParameterFormatter implements DiscogsJobParameterFormatter {

    private static final Long DEFAULT_CHUNK_SIZE = 10000L;
    private static final String CHUNK_SIZE_KEY = "chunkSize";
    private static final String YEAR_MONTH_KEY = "yearMonth";
    private static final String ETAG_KEY = "etag";
    private final DumpService dumpService;

    @Override
    public JobParameters format(JobParameters parameters) {
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(CHUNK_SIZE_KEY, extractChunkSize(parameters));
        if (isEtagPresent(parameters)) {

        }

        return null;
    }

    private JobParameters extractEtagList(JobParameters parameters) {
        String fullStr = getString(parameters, ETAG_KEY);
        assert fullStr != null;
        List<String> etagList = Arrays.asList(fullStr.split(","));

        Map<DumpType, DiscogsDump> dumpMap = etagList.stream()
                .map(dumpService::getDumpByEtag)
                .collect(Collectors.toMap(DiscogsDump::getDumpType, dump -> dump));


    }

    private Map<DumpType, DiscogsDump> resolveDumpDependency(Map<DumpType, DiscogsDump> dumpMap) {
        Map<DumpType, DiscogsDump> resultMap = new HashMap<>();
        dumpMap.forEach(resultMap::put);

        if (resultMap.containsKey(DumpType.RELEASE)) {
            DiscogsDump releaseDump = resultMap.get(DumpType.RELEASE);
            LocalDate targetDate = releaseDump.getLastModified().toZonedDateTime().toLocalDate();

            log.info("found release dump type. fetching relevant dumps from: " + targetDate.getMonth() + ", " + targetDate.getYear());
            List<DiscogsDump> list = dumpService.getDumpListInYearMonth(targetDate.getYear(), targetDate.getMonthValue());
            list.stream()
                    .filter(item -> item.getDumpType() != DumpType.RELEASE)
                    .forEach(item -> {
                        if (resultMap.containsKey(item.getDumpType())) {
                            DiscogsDump preListedDump = resultMap.get(item.getDumpType());
                            if (item.getLastModified().isAfter(preListedDump.getLastModified())) {
                                log.debug("replacing dump {{}} to {{}}", preListedDump.getEtag(), item.getEtag());
                                resultMap.put(item.getDumpType(), item);
                            }
                        }
                    });
            return resultMap;
        }

        if (resultMap.containsKey(DumpType.MASTER)) {
            DiscogsDump masterDump = resultMap.get(DumpType.MASTER);
            OffsetDateTime time = masterDump.getLastModified().withOffsetSameInstant(ZoneOffset.UTC);
            List<DiscogsDump> list = dumpService.getDumpListInYearMonth(time.getYear(), time.getMonthValue());
        }
    }

    private boolean isEtagPresent(JobParameters parameters) {
        String key = getKeyEqualsIgnoreCase(parameters, ETAG_KEY);
        if (key == null) {
            log.debug("etag entry not found.");
            return false;
        }
        String value = parameters.getString(key);
        if (value == null || value.isBlank()) {
            log.debug("found etag entry with empty value.");
            return false;
        }
        return true;
    }

    private boolean isValidYearMonthPresent(JobParameters jobParameters) {
        String key = getKeyEqualsIgnoreCase(jobParameters, YEAR_MONTH_KEY);
        String date = jobParameters.getString(key);
        if (date == null || date.isBlank()) {
            return false;
        }
        return MalformedDateParser.yearMonthMatches(date);
    }


    private Map<String, String> checkRecentDumps() {
        log.debug("fetching recent dumps");
        List<DiscogsDump> recentDumps = dumpService.getLatestCompletedDumpSet();

        Map<String, String> etagMap = new HashMap<>();
        recentDumps.forEach(dump -> {
            etagMap.put(dump.getRootElementName(), dump.getEtag());
        });

        return etagMap;
    }

    /**
     * @param jobParameters given JobParameters
     * @return result of finding whether to run recent or not
     */
    private boolean isRecentOrder(JobParameters jobParameters) {
        String key = getKeyEqualsIgnoreCase(jobParameters, "recent");
        if (key != null) {
            String val = jobParameters.getString(key);
            if (val != null) {
                return !val.equalsIgnoreCase("false");
            }
        }
        return false;
    }


    private long extractChunkSize(JobParameters origin) {
        if (getKeyEqualsIgnoreCase(origin, CHUNK_SIZE_KEY) == null) {
            return DEFAULT_CHUNK_SIZE;
        }

        Long chunkSize = getLong(origin, CHUNK_SIZE_KEY);

        if (chunkSize == null) {
            log.debug("found chunkSize entry but is empty: {{}}. setting to default {{}}.",
                    getKeyEqualsIgnoreCase(origin, CHUNK_SIZE_KEY),
                    DEFAULT_CHUNK_SIZE);
            return DEFAULT_CHUNK_SIZE;
        }
        log.debug("setting chunkSize to {{}}.", chunkSize);
        return chunkSize;
    }

    private Long getLong(JobParameters parameters, String key) {
        String entryKey = getKeyEqualsIgnoreCase(parameters, key);
        if (entryKey == null) {
            return null;
        }
        return parameters.getLong(entryKey);
    }

    private String getString(JobParameters parameters, String key) {
        String entryKey = getKeyEqualsIgnoreCase(parameters, key);
        if (entryKey == null) {
            return null;
        }
        return parameters.getString(entryKey);
    }

    private String getKeyEqualsIgnoreCase(JobParameters parameters, String key) {
        return parameters.getParameters().keySet().stream()
                .filter(s -> s.equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }
}
