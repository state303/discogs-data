package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterGenre;
import io.dsub.discogsdata.common.entity.master.MasterStyle;
import io.dsub.discogsdata.common.repository.master.MasterGenreRepository;
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
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class MasterGenreStepConfigurer {

    private final MasterGenreRepository masterGenreRepository;
    private final DumpCache dumpCache;
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @JobScope
    public Step masterGenreStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterStyleStep " + etag)
                .<SimpleRelation, Future<MasterGenre>>chunk(chunkSize)
                .reader(masterGenreReader())
                .processor(asyncMasterGenreProcessor())
                .writer(asyncMasterGenreWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> masterGenreReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                dumpCache.pullSimpleRelationsQueue(MasterGenre.class);
        return queue::poll;
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<SimpleRelation, MasterGenre> asyncMasterGenreProcessor() throws Exception {
        ItemProcessor<SimpleRelation, MasterGenre> processor = item -> {
            Master master = Master.builder().id((Long)item.getParent()).build();
            Genre genre = Genre.builder().id(dumpCache.getGenreId(String.valueOf(item.getChild()))).build();
            return MasterGenre.builder()
                    .master(master)
                    .genre(genre)
                    .build();
        };

        AsyncItemProcessor<SimpleRelation, MasterGenre> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(processor);
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<MasterGenre> asyncMasterGenreWriter() throws Exception {
        RepositoryItemWriter<MasterGenre> writer = new RepositoryItemWriterBuilder<MasterGenre>()
                .repository(masterGenreRepository)
                .build();
        AsyncItemWriter<MasterGenre> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(writer);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }
}
