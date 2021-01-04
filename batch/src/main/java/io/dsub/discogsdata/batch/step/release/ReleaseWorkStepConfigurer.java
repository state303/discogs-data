package io.dsub.discogsdata.batch.step.release;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.common.entity.release.ReleaseWork;
import io.dsub.discogsdata.common.repository.label.LabelRepository;
import io.dsub.discogsdata.common.repository.release.ReleaseRepository;
import io.dsub.discogsdata.common.repository.release.ReleaseWorkRepository;
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
public class ReleaseWorkStepConfigurer {

    private final ReleaseWorkRepository releaseWorkRepository;
    private final ReleaseRepository releaseRepository;
    private final LabelRepository labelRepository;
    private final StepBuilderFactory stepBuilderFactory;
    private final DumpCache dumpCache;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @JobScope
    public Step releaseWorkStep(@Value("#{jobParameters['release']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("releaseWorkStep " + etag)
                .<ReleaseWork, Future<ReleaseWork>>chunk(chunkSize)
                .reader(releaseWorkItemReader())
                .processor(asyncReleaseWorkItemProcessor())
                .writer(asyncReleaseWorkWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<ReleaseWork> releaseWorkItemReader() {
        ConcurrentLinkedQueue<ReleaseWork> queue =
                dumpCache.pullObjectRelationsQueue(ReleaseWork.class);
        return queue::poll;
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<ReleaseWork, ReleaseWork> asyncReleaseWorkItemProcessor() throws Exception {
        ItemProcessor<ReleaseWork, ReleaseWork> processor = item -> {
            if (item.getReleaseItem() == null ||
                    item.getLabel() == null ||
                    !releaseRepository.existsById(item.getReleaseItem().getId()) ||
                    !labelRepository.existsById(item.getLabel().getId())) {
                return null;
            }

            if (releaseWorkRepository.existsByLabelAndReleaseItem(item.getLabel(), item.getReleaseItem())) {
                return null;
            }
            return item;
        };

        AsyncItemProcessor<ReleaseWork, ReleaseWork> asyncReleaseArtistProcessor = new AsyncItemProcessor<>();
        asyncReleaseArtistProcessor.setDelegate(processor);
        asyncReleaseArtistProcessor.setTaskExecutor(taskExecutor);
        asyncReleaseArtistProcessor.afterPropertiesSet();
        return asyncReleaseArtistProcessor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<ReleaseWork> asyncReleaseWorkWriter() throws Exception {
        RepositoryItemWriter<ReleaseWork> releaseWorkRepositoryItemWriter = new RepositoryItemWriter<>();
        releaseWorkRepositoryItemWriter.setRepository(releaseWorkRepository);
        releaseWorkRepositoryItemWriter.afterPropertiesSet();
        AsyncItemWriter<ReleaseWork> asyncReleaseWorkWriter = new AsyncItemWriter<>();
        asyncReleaseWorkWriter.setDelegate(releaseWorkRepositoryItemWriter);
        asyncReleaseWorkWriter.afterPropertiesSet();
        return asyncReleaseWorkWriter;
    }

}
