package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.common.entity.Video;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import io.dsub.discogsdata.common.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.MethodInvoker;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class MasterVideoPreStepConfigurer {
    private final VideoRepository videoRepository;
    private final RelationsHolder relationsHolder;
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;

    @JobScope
    @Bean
    public Step masterVideoPreStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterVideoPreStep " + etag)
                .<MasterVideo, Future<MasterVideo>>chunk(chunkSize)
                .reader(masterVideoUrlReader())
                .processor(masterVideoUrlProcessor())
                .writer(masterVideoUrlWriter())
                .taskExecutor(taskExecutor)
                .throttleLimit(10)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<MasterVideo> masterVideoUrlReader() {
        ConcurrentLinkedQueue<MasterVideo> queue =
                relationsHolder.pullObjectRelationsQueue(MasterVideo.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemProcessor<MasterVideo, MasterVideo> masterVideoUrlProcessor() throws Exception {
        ItemProcessor<MasterVideo, MasterVideo> syncProcessor = item -> item;
        AsyncItemProcessor<MasterVideo, MasterVideo> masterVideoUrlProcessor = new AsyncItemProcessor<>();
        masterVideoUrlProcessor.setTaskExecutor(taskExecutor);
        masterVideoUrlProcessor.setDelegate(syncProcessor);
        masterVideoUrlProcessor.afterPropertiesSet();
        return masterVideoUrlProcessor;
    }

    @Bean
    public AsyncItemWriter<MasterVideo> masterVideoUrlWriter() throws Exception {
        RepositoryItemWriter<MasterVideo> syncWriter = new RepositoryItemWriter<>() {
            @Override
            protected void doWrite(List<? extends MasterVideo> items) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Writing to the repository with " + items.size() + " items.");
                }
                List<Video> savedEntities = videoRepository.saveAll(items.stream()
                        .map(MasterVideo::getVideo)
                        .collect(Collectors.toList()));

                items.sort(Comparator.comparing(item -> item.getVideo().getUrl(), String::compareTo));
                savedEntities.sort(Comparator.comparing(Video::getUrl, String::compareTo));

                for (int i = 0; i < items.size(); i++) {
                    Long videoId = savedEntities.get(i).getId();
                    Long masterId = items.get(i).getMaster().getId();
                    relationsHolder.addSimpleRelation("TemporaryMasterVideo", new SimpleRelation(masterId, videoId));
                }
            }
        };

        AsyncItemWriter<MasterVideo> masterVideoUrlWriter = new AsyncItemWriter<>();
        masterVideoUrlWriter.setDelegate(syncWriter);
        masterVideoUrlWriter.afterPropertiesSet();
        return masterVideoUrlWriter;
    }
}
