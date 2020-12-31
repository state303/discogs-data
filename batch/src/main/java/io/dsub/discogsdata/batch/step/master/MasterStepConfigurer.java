package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.reader.CustomStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.repository.master.MasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class MasterStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final MasterRepository masterRepository;
    private final XmlMasterReadListener readListener;

    @Bean
    @JobScope
    public Step masterStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterStep " + etag)
                .<XmlMaster, Future<Master>>chunk(chunkSize)
                .reader(masterReader(null))
                .processor(asyncMasterProcessor())
                .writer(asyncMasterWriter())
                .listener(readListener)
                .stream(masterReader(null))
                .taskExecutor(taskExecutor)
                .throttleLimit(10)
                .build();
    }

    @Bean
    @StepScope
    public CustomStaxEventItemReader<XmlMaster> masterReader(@Value("#{jobParameters['master']}") String etag) throws Exception {
        DiscogsDump labelDump = dumpService.getDumpByEtag(etag);
        return new CustomStaxEventItemReader<>(XmlMaster.class, labelDump);
    }

    @Bean
    public AsyncItemProcessor<XmlMaster, Master> asyncMasterProcessor() {
        AsyncItemProcessor<XmlMaster, Master> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(XmlMaster::toEntity);
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<Master> asyncMasterWriter() throws Exception {
        RepositoryItemWriter<Master> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(masterRepository);
        syncWriter.afterPropertiesSet();

        AsyncItemWriter<Master> asyncMasterWriter = new AsyncItemWriter<>();
        asyncMasterWriter.setDelegate(syncWriter);
        asyncMasterWriter.afterPropertiesSet();
        return asyncMasterWriter;
    }

}
