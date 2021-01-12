package io.dsub.discogsdata.batch.step.artist.jdbc;

import io.dsub.discogsdata.batch.step.artist.ArtistDataProcessor;
import io.dsub.discogsdata.batch.step.artist.ArtistDumpReader;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
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
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class ArtistJdbcStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final ArtistJdbcItemWriter artistJdbcItemWriter;
    private final TaskExecutor taskExecutor;
    private final ArtistDataProcessor artistDataProcessor;
    private final ArtistDumpReader artistDumpReader;

    @Bean
    @JobScope
    public Step artistJdbcStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") Integer chunkSize) throws Exception {
        return stepBuilderFactory.get("artistJdbcStep " + etag)
                .<XmlArtist, Future<XmlArtist>>chunk(chunkSize)
                .reader(artistDumpReader)
                .processor(asyncArtistDataProcessor())
                .writer(artistJdbcStepWriter())
                .build();
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<XmlArtist, XmlArtist> asyncArtistDataProcessor() throws Exception {
        AsyncItemProcessor<XmlArtist, XmlArtist> processor = new AsyncItemProcessor<>();
        processor.setTaskExecutor(taskExecutor);
        processor.setDelegate(artistDataProcessor);
        processor.afterPropertiesSet();
        return processor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<XmlArtist> artistJdbcStepWriter() throws Exception {
        AsyncItemWriter<XmlArtist> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(artistJdbcItemWriter);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }
}
