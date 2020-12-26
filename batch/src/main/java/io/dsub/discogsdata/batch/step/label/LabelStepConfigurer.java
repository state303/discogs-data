package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.process.XmlObjectReadListener;
import io.dsub.discogsdata.batch.reader.CustomStaxEventItemReader;
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
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.persistence.EntityManagerFactory;
import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class LabelStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final LabelRepository labelRepository;
    private final XmlObjectReadListener xmlObjectReadListener;

    @Bean
    @JobScope
    public Step labelStep(@Value("#{jobParameters['label']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {

        return stepBuilderFactory
                .get("labelStep " + etag)
                .<XmlLabel, Future<Label>>chunk(chunkSize)
                .reader(labelReader(null))
                .processor(asyncLabelProcessor())
                .writer(asyncLabelWriter())
                .stream(labelReader(null))
                .listener(xmlObjectReadListener)
                .taskExecutor(taskExecutor)
                .throttleLimit(100)
                .build();
    }

    @Bean
    @StepScope
    public CustomStaxEventItemReader<XmlLabel> labelReader(@Value("#{jobParameters['label']}") String etag) throws Exception {
        DiscogsDump labelDump = dumpService.getDumpByEtag(etag);
        return new CustomStaxEventItemReader<>(XmlLabel.class, labelDump);
    }

    @Bean
    public AsyncItemProcessor<XmlLabel, Label> asyncLabelProcessor() {
        AsyncItemProcessor<XmlLabel, Label> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(XmlLabel::toEntity);
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
