package io.dsub.discogsdata.batch.reader;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import io.dsub.discogsdata.common.entity.artist.ArtistMember;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Slf4j
@RequiredArgsConstructor
public class CustomStaxEventItemReader<T> implements ItemReader<T>, ItemStream, InitializingBean, ResourceAwareItemReaderItemStream<T> {

    private static volatile AtomicInteger count = new AtomicInteger(0);
    private final StaxEventItemReader<T> nestedReader;
    private final RelationsHolder relationsHolder;

    @Override
    public synchronized T read() throws Exception {
        count.addAndGet(1);

        if (500 < count.get()) {
            log.debug("5000 exceeded. returning null object");
//            count = new AtomicInteger(0);
            return null;
        }

        return nestedReader.read();
    }

    @Override
    public void setResource(Resource resource) {
        nestedReader.setResource(resource);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        nestedReader.afterPropertiesSet();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        nestedReader.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        nestedReader.update(executionContext);
    }

    @Override
    public void close() {
        nestedReader.close();
    }

    public boolean isSaveState() {
        return nestedReader.isSaveState();
    }

    public void setSaveState(boolean saveState) {
        nestedReader.setSaveState(saveState);
    }

    public void setStrict(boolean strict) {
        nestedReader.setStrict(strict);
    }

    public void setCurrentItemCount(int count) {
        nestedReader.setCurrentItemCount(count);
    }

    public void setFragmentRootElementName(String fragmentRootElementName) {
        nestedReader.setFragmentRootElementName(fragmentRootElementName);
    }

    public void setMaxItemCount(int count) {
        nestedReader.setMaxItemCount(count);
    }

    public void setUnmarshaller(Unmarshaller unmarshaller) {
        nestedReader.setUnmarshaller(unmarshaller);
    }

    public void setName(String name) {
        nestedReader.setName(name);
    }
}
