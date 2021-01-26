package io.dsub.discogsdata.batch.step.release;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlReleaseItemDetails;
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
public class ReleaseItemJdbcPostStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ReleaseItemDetailsJdbcWriter writer;

    @Bean
    @JobScope
    public Step releaseItemJdbcPostStep(@Value("#{jobParameters['release']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("releaseItemJdbcPostStep")
                .<XmlReleaseItemDetails, Future<XmlReleaseItemDetails>>chunk(chunkSize)
                .reader(releaseItemJdbcPostReader(null))
                .processor(releaseItemJdbcPostStepProcessor())
                .writer(releaseItemJdbcPostStepWriter())
                .build();
    }

    @Bean
    @StepScope
    public ProgressBarStaxEventItemReader<XmlReleaseItemDetails> releaseItemJdbcPostReader(@Value("#{jobParameters['release']}") String etag) throws Exception {
        ProgressBarStaxEventItemReader<XmlReleaseItemDetails> reader =
                new ProgressBarStaxEventItemReader<>(XmlReleaseItemDetails.class, dumpService.getDumpByEtag(etag));
        reader.afterPropertiesSet();
        return reader;
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<XmlReleaseItemDetails, XmlReleaseItemDetails> releaseItemJdbcPostStepProcessor() throws Exception {
        AsyncItemProcessor<XmlReleaseItemDetails, XmlReleaseItemDetails> processor =
                new AsyncItemProcessor<>();
        processor.setDelegate(item -> item);
        processor.afterPropertiesSet();
        processor.setTaskExecutor(taskExecutor);
        return processor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<XmlReleaseItemDetails> releaseItemJdbcPostStepWriter() throws Exception {
        AsyncItemWriter<XmlReleaseItemDetails> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(writer);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }

}
