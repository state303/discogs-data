package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import org.w3c.dom.NodeList;

import java.util.List;

public interface DumpFetcher {
    List<DiscogsDump> getDiscogsDumps();
    DiscogsDump parseDump(NodeList dataNodeList);
}
