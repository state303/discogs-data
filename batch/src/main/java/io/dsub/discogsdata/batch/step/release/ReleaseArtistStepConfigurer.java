package io.dsub.discogsdata.batch.step.release;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.common.entity.release.ReleaseArtist;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import io.dsub.discogsdata.common.repository.release.ReleaseArtistRepository;
import io.dsub.discogsdata.common.repository.release.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
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
public class ReleaseArtistStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpCache dumpCache;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ArtistRepository artistRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseArtistRepository releaseArtistRepository;

    @Bean
    @StepScope
    public Step releaseArtistStep(@Value("#{jobParameters['release']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("releaseArtistStep " + etag)
                .<ReleaseArtist, Future<ReleaseArtist>>chunk(chunkSize)
                .reader(releaseArtistItemReader())
                .processor(asyncReleaseArtistItemProcessor())
                .writer(asyncReleaseArtistWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<ReleaseArtist> releaseArtistItemReader() {
        ConcurrentLinkedQueue<ReleaseArtist> queue =
                dumpCache.pullObjectRelationsQueue(ReleaseArtist.class);
        return queue::poll;
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<ReleaseArtist, ReleaseArtist> asyncReleaseArtistItemProcessor() throws Exception {
        ItemProcessor<ReleaseArtist, ReleaseArtist> processor = item -> {
            if (item.getReleaseItem() == null ||
                    item.getArtist() == null ||
                    !releaseRepository.existsById(item.getReleaseItem().getId()) ||
                    !artistRepository.existsById(item.getArtist().getId())) {
                return null;
            }

            if (releaseArtistRepository.existsByArtistAndReleaseItem(item.getArtist(), item.getReleaseItem())) {
                return null;
            }

            return item;
        };

        AsyncItemProcessor<ReleaseArtist, ReleaseArtist> asyncReleaseArtistProcessor = new AsyncItemProcessor<>();
        asyncReleaseArtistProcessor.setDelegate(processor);
        asyncReleaseArtistProcessor.setTaskExecutor(taskExecutor);
        asyncReleaseArtistProcessor.afterPropertiesSet();
        return asyncReleaseArtistProcessor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<ReleaseArtist> asyncReleaseArtistWriter() throws Exception {
        RepositoryItemWriter<ReleaseArtist> releaseArtistRepositoryItemWriter = new RepositoryItemWriter<>();
        releaseArtistRepositoryItemWriter.setRepository(releaseArtistRepository);
        releaseArtistRepositoryItemWriter.afterPropertiesSet();
        AsyncItemWriter<ReleaseArtist> asyncReleaseArtistWriter = new AsyncItemWriter<>();
        asyncReleaseArtistWriter.setDelegate(releaseArtistRepositoryItemWriter);
        asyncReleaseArtistWriter.afterPropertiesSet();
        return asyncReleaseArtistWriter;
    }
}
