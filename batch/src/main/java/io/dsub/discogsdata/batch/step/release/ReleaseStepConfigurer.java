package io.dsub.discogsdata.batch.step.release;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlRelease;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import io.dsub.discogsdata.common.repository.release.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.*;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class ReleaseStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final ReleaseRepository releaseRepository;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @JobScope
    public Step releaseStep(@Value("#{jobParameters['release']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("releaseStep " + etag)
                .<XmlRelease, Future<ReleaseItem>>chunk(chunkSize)
                .reader(releaseItemReader(null))
                .processor(asyncReleaseItemProcessor())
                .writer(asyncReleaseItemWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ProgressBarStaxEventItemReader<XmlRelease> releaseItemReader(@Value("#{jobParameters['release']}") String etag) throws Exception {
        DiscogsDump releaseDump = dumpService.getDumpByEtag(etag);
        return new ProgressBarStaxEventItemReader<>(XmlRelease.class, releaseDump);
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<XmlRelease, ReleaseItem> asyncReleaseItemProcessor() throws Exception {
        ItemProcessor<XmlRelease, ReleaseItem> processor = XmlRelease::toEntity;
        AsyncItemProcessor<XmlRelease, ReleaseItem> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(processor);
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<ReleaseItem> asyncReleaseItemWriter() throws Exception {
        RepositoryItemWriter<ReleaseItem> repositoryItemWriter = new RepositoryItemWriterBuilder<ReleaseItem>()
                .repository(releaseRepository)
                .build();

        AsyncItemWriter<ReleaseItem> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(repositoryItemWriter);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }
}
