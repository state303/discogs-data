package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;

import java.util.Collection;
import java.util.List;

public interface DumpDependencyResolver {
    List<DiscogsDump> resolveByEtag(Collection<String> etag);
    List<DiscogsDump> resolveByType(Collection<String> types);
    List<DiscogsDump> resolveByYearMonth(String yearMonth);
    List<DiscogsDump> resolveByTypeAndYearMonth(Collection<String> types, String yearMonth);
}