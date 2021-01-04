package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterStyle;
import io.dsub.discogsdata.common.repository.master.MasterStyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class MasterStyleStepConfigurer {

    private final MasterStyleRepository masterStyleRepository;
    private final DumpCache dumpCache;
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @JobScope
    public Step masterStyleStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterStyleStep " + etag)
                .<SimpleRelation, Future<MasterStyle>>chunk(chunkSize)
                .reader(masterStyleReader())
                .processor(asyncMasterStyleProcessor())
                .writer(asyncMasterStyleWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> masterStyleReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                dumpCache.pullSimpleRelationsQueue(MasterStyle.class);
        return queue::poll;
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<SimpleRelation, MasterStyle> asyncMasterStyleProcessor() throws Exception {
        ItemProcessor<SimpleRelation, MasterStyle> processor = item -> {
            Master master = Master.builder().id((Long)item.getParent()).build();
            Style style = Style.builder().id(dumpCache.getStyleId(String.valueOf(item.getChild()))).build();
            return MasterStyle.builder()
                    .master(master)
                    .style(style)
                    .build();
        };

        AsyncItemProcessor<SimpleRelation, MasterStyle> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(processor);
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<MasterStyle> asyncMasterStyleWriter() throws Exception {
        RepositoryItemWriter<MasterStyle> writer = new RepositoryItemWriterBuilder<MasterStyle>()
                .repository(masterStyleRepository)
                .build();
        AsyncItemWriter<MasterStyle> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(writer);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }
}
