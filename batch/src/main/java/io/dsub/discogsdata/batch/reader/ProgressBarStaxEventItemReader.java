package io.dsub.discogsdata.batch.reader;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

@Slf4j
@RequiredArgsConstructor
public class ProgressBarStaxEventItemReader<T> implements ItemReader<T>, ItemStream, InitializingBean, ResourceAwareItemReaderItemStream<T> {

    private final StaxEventItemReader<T> nestedReader;
    private static final ProgressBarBuilder pbb = new ProgressBarBuilder();

    public ProgressBarStaxEventItemReader(Class<T> clazz, DiscogsDump dump) throws Exception {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(clazz);
        jaxb2Marshaller.afterPropertiesSet();

        pbb.setTaskName("reading " + dump.getUri().split("/")[2])
                .showSpeed()
                .setUpdateIntervalMillis(1000)
                .setStyle(ProgressBarStyle.UNICODE_BLOCK)
                .setInitialMax(dump.getSize());

        Path target = Path.of(dump.getUri().split("/")[2]);

        InputStream inputStream = new GZIPInputStream(ProgressBar.wrap(Files.newInputStream(target), pbb));

        this.nestedReader = new StaxEventItemReaderBuilder<T>()
                .resource(new InputStreamResource(inputStream))
                .name(clazz.getSimpleName() + " reader " + dump.getEtag())
                .addFragmentRootElements(dump.getRootElementName())
                .unmarshaller(jaxb2Marshaller)
                .build();

        this.afterPropertiesSet();
    }

    @Override
    public synchronized T read() throws Exception {
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
    public synchronized void open(ExecutionContext executionContext) throws ItemStreamException {
        nestedReader.open(executionContext);
    }

    @Override
    public synchronized void update(ExecutionContext executionContext) throws ItemStreamException {
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
