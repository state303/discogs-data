package io.dsub.discogsdata.batch.step.release;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.common.entity.release.ReleaseVideo;
import io.dsub.discogsdata.common.repository.release.ReleaseVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemWriter;
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
public class ReleaseVideoStepConfigurer {

    private final ReleaseVideoRepository releaseVideoRepository;
    private final StepBuilderFactory stepBuilderFactory;
    private final DumpCache dumpCache;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @JobScope
    public Step releaseVideoStep(@Value("#{jobParameters['release']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("releaseVideoStep " + etag)
                .<ReleaseVideo, Future<ReleaseVideo>>chunk(chunkSize)
                .reader(releaseVideoReader())
                .writer(asyncReleaseVideoWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<ReleaseVideo> releaseVideoReader() {
        ConcurrentLinkedQueue<ReleaseVideo> queue =
                dumpCache.pullObjectRelationsQueue(ReleaseVideo.class);
        return queue::poll;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<ReleaseVideo> asyncReleaseVideoWriter() throws Exception {
        RepositoryItemWriter<ReleaseVideo> writer = new RepositoryItemWriter<>();
        writer.setRepository(releaseVideoRepository);
        writer.afterPropertiesSet();

        AsyncItemWriter<ReleaseVideo> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(writer);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }

}
