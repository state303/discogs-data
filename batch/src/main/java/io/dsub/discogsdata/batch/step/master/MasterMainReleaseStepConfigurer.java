package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import io.dsub.discogsdata.common.repository.master.MasterRepository;
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
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class MasterMainReleaseStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final DumpCache dumpCache;
    private final MasterRepository masterRepository;
    private final ReleaseRepository releaseRepository;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @StepScope
    public Step masterMainReleaseStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterMainReleaseStep " + etag)
                .<SimpleRelation, Future<Master>>chunk(chunkSize)
                .reader(masterMainReleaseReader())
                .processor(asyncMasterMainReleaseProcessor())
                .writer(masterMainReleaseWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> masterMainReleaseReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                dumpCache.pullSimpleRelationsQueue("masterMainRelease");
        return queue::poll;
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<SimpleRelation, Master> asyncMasterMainReleaseProcessor() throws Exception {
        ItemProcessor<SimpleRelation, Master> syncProcessor = item -> {
            if (item.getParent() == null ||
                    item.getChild() == null ||
                    !releaseRepository.existsById((Long) item.getChild())) {
                return null;
            }
            Master master = masterRepository.findById((Long) item.getParent()).orElse(null);
            ReleaseItem releaseItem = releaseRepository.findById((Long) item.getChild()).orElse(null);
            if (releaseItem == null || master == null) {
                return null;
            }

            master.setMainReleaseItem(releaseItem);
            return master;
        };

        AsyncItemProcessor<SimpleRelation, Master> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(syncProcessor);
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<Master> masterMainReleaseWriter() throws Exception {
        RepositoryItemWriter<Master> writer = new RepositoryItemWriterBuilder<Master>()
                .repository(masterRepository)
                .build();
        AsyncItemWriter<Master> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(writer);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }
}
