package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import io.dsub.discogsdata.common.entity.label.Label;
import io.dsub.discogsdata.common.entity.label.LabelSubLabel;
import io.dsub.discogsdata.common.repository.label.LabelRepository;
import io.dsub.discogsdata.common.repository.label.LabelSubLabelRepository;
import lombok.RequiredArgsConstructor;
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

@Configuration
@RequiredArgsConstructor
public class SubLabelStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final LabelRepository labelRepository;
    private final LabelSubLabelRepository labelSubLabelRepository;
    private final RelationsHolder relationsHolder;

    @Bean
    @JobScope
    public Step subLabelStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory
                .get("subLabelStep " + etag)
                .<SimpleRelation, Future<LabelSubLabel>>chunk(chunkSize)
                .reader(subLabelReader())
                .processor(asyncSubLabelProcessor())
                .writer(asyncLabelSubLabelWriter())
                .taskExecutor(taskExecutor)
                .throttleLimit(10)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> subLabelReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                relationsHolder.pullSimpleRelationsQueue(XmlLabel.SubLabel.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemProcessor<SimpleRelation, LabelSubLabel> asyncSubLabelProcessor() throws Exception {
        ItemProcessor<SimpleRelation, LabelSubLabel> syncProcessor = item -> {
            if (item.getParentId() != null &&
                    item.getChildId() != null &&
                    labelRepository.existsById(item.getParentId()) &&
                    labelRepository.existsById(item.getChildId())) {

                Label parentLabel = labelRepository.getOne(item.getParentId());
                Label childLabel = labelRepository.getOne(item.getChildId());

                if (!labelSubLabelRepository.existsByParentAndSubLabel(parentLabel, childLabel)) {
                    LabelSubLabel entity = new LabelSubLabel();
                    entity.setParent(labelRepository.getOne(item.getParentId()));
                    entity.setSubLabel(labelRepository.getOne(item.getChildId()));
                    return entity;
                }
            }
            return null;
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
