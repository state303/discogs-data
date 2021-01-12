package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class LabelJdbcStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final LabelDumpReader labelDumpReader;
    private final LabelDataProcessor labelDataProcessor;
    private final LabelJdbcItemWriter labelJdbcItemWriter;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @JobScope
    public Step labelJdbcStep(@Value("#{jobParameters['label']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {

        return stepBuilderFactory
                .get("labelStep " + etag)
                .<XmlLabel, Future<XmlLabel>>chunk(chunkSize)
                .reader(labelDumpReader)
                .processor(asyncLabelJdbcProcessor())
                .writer(asyncLabelJdbcWriter())
                .build();
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<XmlLabel, XmlLabel> asyncLabelJdbcProcessor() throws Exception {
        AsyncItemProcessor<XmlLabel, XmlLabel> processor = new AsyncItemProcessor<>();
        processor.setTaskExecutor(taskExecutor);
        processor.setDelegate(labelDataProcessor);
        processor.afterPropertiesSet();
        return processor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<XmlLabel> asyncLabelJdbcWriter() throws Exception {
        AsyncItemWriter<XmlLabel> writer = new AsyncItemWriter<>();
        writer.setDelegate(labelJdbcItemWriter);
        writer.afterPropertiesSet();
        return writer;
    }

}
