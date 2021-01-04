package io.dsub.discogsdata.batch.step.release;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.common.entity.release.ReleaseCreditedArtist;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import io.dsub.discogsdata.common.repository.release.ReleaseCreditedArtistRepository;
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
public class ReleaseCreditedArtistStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpCache dumpCache;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ArtistRepository artistRepository;
    private final ReleaseRepository releaseRepository;
    private final ReleaseCreditedArtistRepository releaseCreditedArtistRepository;

    @Bean
    @StepScope
    public Step releaseCreditedArtistStep(@Value("#{jobParameters['release']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("releaseCreditedArtistStep " + etag)
                .<ReleaseCreditedArtist, Future<ReleaseCreditedArtist>>chunk(chunkSize)
                .reader(releaseCreditedArtistReader())
                .processor(asyncReleaseCreditedArtistItemProcessor())
                .writer(asyncReleaseCreditedArtistWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<ReleaseCreditedArtist> releaseCreditedArtistReader() {
        ConcurrentLinkedQueue<ReleaseCreditedArtist> queue =
                dumpCache.pullObjectRelationsQueue(ReleaseCreditedArtist.class);
        return queue::poll;
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<ReleaseCreditedArtist, ReleaseCreditedArtist> asyncReleaseCreditedArtistItemProcessor() throws Exception {
        ItemProcessor<ReleaseCreditedArtist, ReleaseCreditedArtist> processor = item -> {
            if (item.getReleaseItem() == null ||
                    item.getArtist() == null ||
                    !releaseRepository.existsById(item.getReleaseItem().getId()) ||
                    !artistRepository.existsById(item.getArtist().getId())) {
                return null;
            }

            if (releaseCreditedArtistRepository.existsByArtistAndReleaseItem(item.getArtist(), item.getReleaseItem())) {
                return null;
            }
            return item;
        };

        AsyncItemProcessor<ReleaseCreditedArtist, ReleaseCreditedArtist> asyncReleaseArtistProcessor = new AsyncItemProcessor<>();
        asyncReleaseArtistProcessor.setDelegate(processor);
        asyncReleaseArtistProcessor.setTaskExecutor(taskExecutor);
        asyncReleaseArtistProcessor.afterPropertiesSet();
        return asyncReleaseArtistProcessor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<ReleaseCreditedArtist> asyncReleaseCreditedArtistWriter() throws Exception {
        RepositoryItemWriter<ReleaseCreditedArtist> releaseCreditedArtistRepositoryItemWriter = new RepositoryItemWriter<>();
        releaseCreditedArtistRepositoryItemWriter.setRepository(releaseCreditedArtistRepository);
        releaseCreditedArtistRepositoryItemWriter.afterPropertiesSet();
        AsyncItemWriter<ReleaseCreditedArtist> asyncReleaseCreditedArtistWriter = new AsyncItemWriter<>();
        asyncReleaseCreditedArtistWriter.setDelegate(releaseCreditedArtistRepositoryItemWriter);
        asyncReleaseCreditedArtistWriter.afterPropertiesSet();
        return asyncReleaseCreditedArtistWriter;
    }
}
