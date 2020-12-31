package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.dump.DumpDependencyResolver;
import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleDiscogsJobParameterResolver implements DiscogsJobParameterResolver {

    private static final Long DEFAULT_CHUNK_SIZE = 10000L;
    private static final String CHUNK_SIZE_KEY = "chunkSize";
    private static final String YEAR_MONTH_KEY = "yearMonth";
    private static final String TYPES_KEY = "types";
    private static final String ETAG_KEY = "etag";
    private final DumpService dumpService;
    private final DumpDependencyResolver resolver;

    @Override
    public JobParameters resolve(JobParameters parameters) {
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(CHUNK_SIZE_KEY, extractChunkSize(parameters));

        Long runId = parameters.getLong("run.id");

        if (runId != null) {
            builder.addLong("run.id", runId);
        }

        if (isValidEntryPresent(parameters, ETAG_KEY)) {
            builder.addJobParameters(extractEtagList(parameters));
            return builder.toJobParameters();
        }

        String yearMonthKey = getKeyEqualsIgnoreCase(parameters, YEAR_MONTH_KEY);
        String typesKey = getKeyEqualsIgnoreCase(parameters, TYPES_KEY);
        boolean haveYearMonth = isValidEntryPresent(parameters, YEAR_MONTH_KEY);
        boolean haveTypes = isValidEntryPresent(parameters, TYPES_KEY);

        if (haveYearMonth && haveTypes) {
            List<String> types = extractStrings(parameters, TYPES_KEY);
            List<DiscogsDump> resolvedList = resolver.resolveByTypeAndYearMonth(
                    types, parameters.getString(yearMonthKey));
            for (DiscogsDump dump : resolvedList) {
                builder.addString(dump.getRootElementName(), dump.getEtag());
            }
            return builder.toJobParameters();
        }

        if (haveTypes) {
            List<String> types = extractStrings(parameters, typesKey);
            resolver.resolveByType(types).forEach(dump ->
                    builder.addString(dump.getRootElementName(), dump.getEtag()));
            return builder.toJobParameters();
        }

        if (haveYearMonth) {
            resolver.resolveByYearMonth(parameters.getString(yearMonthKey))
                    .forEach(dump -> builder.addString(dump.getRootElementName(), dump.getEtag()));
            return builder.toJobParameters();
        }

        dumpService.getLatestCompletedDumpSet().forEach(
                dump -> builder.addString(dump.getRootElementName(), dump.getEtag()));
        return builder.toJobParameters();
    }

    private boolean isValidEntryPresent(JobParameters parameters, String key) {
        String entryKey = getKeyEqualsIgnoreCase(parameters, key);
        if (entryKey == null) {
            log.info("{} entry not found.", key);
            return false;
        }
        String value = parameters.getString(entryKey);
        if (value == null || value.isBlank()) {
            log.info("found {} entry with empty value.", key);
            return false;
        }
        return true;
    }

    private JobParameters extractEtagList(JobParameters userParameters) {
        String fullStr = getString(userParameters, ETAG_KEY);
        assert fullStr != null;
        List<String> etagList = Arrays.asList(fullStr.split(","));
        List<DiscogsDump> resolvedDumps = resolver.resolveByEtag(etagList);
        JobParametersBuilder builder = new JobParametersBuilder();
        resolvedDumps.forEach(dump -> builder.addString(dump.getRootElementName(), dump.getEtag()));
        return builder.toJobParameters();
    }

    private List<String> extractStrings(JobParameters parameters, String key) {
        String target = getKeyEqualsIgnoreCase(parameters, key);
        String value = parameters.getString(target);
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return List.of(value.split(","));
    }

    private long extractChunkSize(JobParameters origin) {
        if (getKeyEqualsIgnoreCase(origin, CHUNK_SIZE_KEY) == null) {
            return DEFAULT_CHUNK_SIZE;
        }

        Long chunkSize = getLong(origin, CHUNK_SIZE_KEY);

        if (chunkSize == null) {
            log.info("found chunkSize entry but is empty: {{}}. setting to default {{}}.",
                    getKeyEqualsIgnoreCase(origin, CHUNK_SIZE_KEY),
                    DEFAULT_CHUNK_SIZE);
            return DEFAULT_CHUNK_SIZE;
        }
        log.info("setting chunkSize to {{}}.", chunkSize);
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
