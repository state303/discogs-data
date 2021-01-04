package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import io.dsub.discogsdata.common.entity.label.Label;
import io.dsub.discogsdata.common.entity.label.LabelSubLabel;
import io.dsub.discogsdata.common.repository.label.LabelRepository;
import io.dsub.discogsdata.common.repository.label.LabelSubLabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SubLabelStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final LabelRepository labelRepository;
    private final LabelSubLabelRepository labelSubLabelRepository;
    private final DumpCache dumpCache;

    @Bean
    @JobScope
    public Step subLabelStep(@Value("#{jobParameters['label']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory
                .get("subLabelStep " + etag)
                .<SimpleRelation, Future<LabelSubLabel>>chunk(chunkSize)
                .reader(subLabelReader())
                .processor(asyncSubLabelProcessor())
                .writer(asyncLabelSubLabelWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> subLabelReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                dumpCache.pullSimpleRelationsQueue(XmlLabel.SubLabel.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemProcessor<SimpleRelation, LabelSubLabel> asyncSubLabelProcessor() throws Exception {
        ItemProcessor<SimpleRelation, LabelSubLabel> syncProcessor = item -> {
            if (item.getParent() == null ||
                    item.getChild() == null ||
                    !labelRepository.existsById((Long) item.getParent()) ||
                    !labelRepository.existsById((Long) item.getChild())) {
                return null;
            }

            Label parent = Label.builder().id((Long) item.getParent()).build();
            Label child = Label.builder().id((Long) item.getChild()).build();

            if (labelSubLabelRepository.existsByParentAndSubLabel(parent, child)) {
                return null;
            }

            return LabelSubLabel.builder()
                    .parent(parent)
                    .subLabel(child)
                    .build();
        };
        AsyncItemProcessor<SimpleRelation, LabelSubLabel> asyncSubLabelProcessor = new AsyncItemProcessor<>();
        asyncSubLabelProcessor.setTaskExecutor(taskExecutor);
        asyncSubLabelProcessor.setDelegate(syncProcessor);
        asyncSubLabelProcessor.afterPropertiesSet();
        return asyncSubLabelProcessor;
    }

    @Bean
    public AsyncItemWriter<LabelSubLabel> asyncLabelSubLabelWriter() throws Exception {
        RepositoryItemWriter<LabelSubLabel> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(labelSubLabelRepository);
        syncWriter.afterPropertiesSet();
        AsyncItemWriter<LabelSubLabel> asyncLabelSubLabelWriter = new AsyncItemWriter<>();
        asyncLabelSubLabelWriter.setDelegate(syncWriter);
        asyncLabelSubLabelWriter.afterPropertiesSet();
        return asyncLabelSubLabelWriter;
    }
}
