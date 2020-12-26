package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import io.dsub.discogsdata.common.repository.VideoRepository;
import io.dsub.discogsdata.common.repository.master.MasterRepository;
import io.dsub.discogsdata.common.repository.master.MasterVideoRepository;
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
public class MasterVideoStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RelationsHolder relationsHolder;
    private final MasterRepository masterRepository;
    private final VideoRepository videoRepository;
    private final MasterVideoRepository masterVideoRepository;

    @Bean
    @JobScope
    public Step masterVideoStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterVideoStep " + etag)
                .<SimpleRelation, Future<MasterVideo>>chunk(chunkSize)
                .reader(masterVideoReader())
                .processor(masterVideoProcessor())
                .writer(masterVideoWriter())
                .taskExecutor(taskExecutor)
                .throttleLimit(10)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> masterVideoReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                relationsHolder.pullSimpleRelationsQueue("MasterVideoTemporary");
        return queue::poll;
    }

    @Bean
    public AsyncItemProcessor<SimpleRelation, MasterVideo> masterVideoProcessor() throws Exception {
        ItemProcessor<SimpleRelation, MasterVideo> syncProcessor = item -> {
            if (item.getParentId() == null ||
                    item.getChildId() == null ||
                    !masterRepository.existsById(item.getParentId()) ||
                    !videoRepository.existsById(item.getChildId())) {
                return null;
            }
            masterRepository.getOne(item.getParentId());
            videoRepository.getOne(item.getChildId());
            return  MasterVideo.builder()
                    .master(masterRepository.getOne(item.getParentId()))
                    .video(videoRepository.getOne(item.getChildId()))
                    .build();
        };

        AsyncItemProcessor<SimpleRelation, MasterVideo> masterVideoProcessor = new AsyncItemProcessor<>();
        masterVideoProcessor.setTaskExecutor(taskExecutor);
        masterVideoProcessor.setDelegate(syncProcessor);
        masterVideoProcessor.afterPropertiesSet();
        return masterVideoProcessor;
    }

    @Bean
    public AsyncItemWriter<MasterVideo> masterVideoWriter() throws Exception {
        RepositoryItemWriter<MasterVideo> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(masterVideoRepository);
        syncWriter.afterPropertiesSet();
        AsyncItemWriter<MasterVideo> masterVideoWriter = new AsyncItemWriter<>();
        masterVideoWriter.setDelegate(syncWriter);
        masterVideoWriter.afterPropertiesSet();
        return masterVideoWriter;
    }
}
