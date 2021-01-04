package io.dsub.discogsdata.batch.step;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.xml.object.XmlObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;

@Slf4j
@RequiredArgsConstructor
public abstract class XmlObjectReadListener<T extends XmlObject> implements ItemReadListener<T> {

    protected static final String DO_NOT_USE_FLAG = "[b]DO NOT USE.[/b]";
    protected final DumpCache dumpCache;

    @Override
    public void beforeRead() {}

    @Override
    public void onReadError(Exception ex) {
        log.debug(ex.getMessage());
    }
}
