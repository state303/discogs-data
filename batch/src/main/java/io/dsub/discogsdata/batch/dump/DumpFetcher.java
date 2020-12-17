package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import org.w3c.dom.NodeList;

import java.util.List;

public interface DumpFetcher {
    List<DiscogsDump> getDiscogsDumps();

    List<DiscogsDump> getDiscogsDumps(String discogsS3BucketUrl);

    DiscogsDump parseDump(NodeList dataNodeList);

    String getS3BucketUrl();
}
