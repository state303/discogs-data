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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
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
                .taskExecutor(taskExecutor)
                .throttleLimit(10)
                .build();
    }

    @Bean
    @StepScope
    public CustomStaxEventItemReader<XmlMaster> masterReader(@Value("#{jobParameters['master']}") String etag) throws Exception {
        DiscogsDump masterDump = dumpService.getDumpByEtag(etag);
        return new CustomStaxEventItemReader<>(XmlMaster.class, masterDump);
    }

    @Bean
    public AsyncItemProcessor<XmlMaster, Master> asyncMasterProcessor() {
        ItemProcessor<XmlMaster, Master> processor = XmlMaster::toEntity;
        AsyncItemProcessor<XmlMaster, Master> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(processor);
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<Master> asyncMasterWriter() throws Exception {
        RepositoryItemWriter<Master> writer = new RepositoryItemWriterBuilder<Master>()
                .repository(masterRepository)
                .build();
        AsyncItemWriter<Master> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(writer);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }
}
