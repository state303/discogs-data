package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import io.dsub.discogsdata.common.entity.label.Label;
import io.dsub.discogsdata.common.repository.label.LabelRepository;
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
public class LabelStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final LabelRepository labelRepository;
    private final LabelDataProcessor labelDataProcessor;

    @Bean
    @JobScope
    public Step labelStep(@Value("#{jobParameters['label']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {

        return stepBuilderFactory
                .get("labelStep " + etag)
                .<XmlLabel, Future<Label>>chunk(chunkSize)
                .reader(labelReader(null))
                .processor(asyncLabelProcessor())
                .writer(asyncLabelWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ProgressBarStaxEventItemReader<XmlLabel> labelReader(@Value("#{jobParameters['label']}") String etag) throws Exception {
        DiscogsDump labelDump = dumpService.getDumpByEtag(etag);
        return new ProgressBarStaxEventItemReader<>(XmlLabel.class, labelDump);
    }

    @Bean
    public AsyncItemProcessor<XmlLabel, Label> asyncLabelProcessor() {
        AsyncItemProcessor<XmlLabel, Label> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(item -> {
            item = labelDataProcessor.process(item);
            if (item == null) {
                return null;
            }
            return Label.builder()
                    .id(item.getId())
                    .dataQuality(item.getDataQuality())
                    .profile(item.getProfile())
                    .name(item.getName())
                    .contactInfo(item.getContactInfo())
                    .urls(item.getUrls())
                    .build();
        });
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<Label> asyncLabelWriter() throws Exception {
        RepositoryItemWriter<Label> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(labelRepository);
        syncWriter.afterPropertiesSet();
        AsyncItemWriter<Label> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(syncWriter);
        asyncWriter.afterPropertiesSet();
        return asyncWriter;
    }
}
