package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.master.MasterGenre;
import io.dsub.discogsdata.common.entity.master.MasterGenre;
import io.dsub.discogsdata.common.repository.GenreRepository;
import io.dsub.discogsdata.common.repository.GenreRepository;
import io.dsub.discogsdata.common.repository.master.MasterGenreRepository;
import io.dsub.discogsdata.common.repository.master.MasterRepository;
import io.dsub.discogsdata.common.repository.master.MasterGenreRepository;
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

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class MasterGenreStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RelationsHolder relationsHolder;
    private final MasterGenreRepository masterGenreRepository;
    private final MasterRepository masterRepository;
    private final GenreRepository genreRepository;
    private final Map<String, Long> genresCache;

    @Bean
    @StepScope
    public Step masterGenreStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterGenreStep " + etag)
                .<MasterGenre, Future<MasterGenre>>chunk(chunkSize)
                .reader(masterGenreReader())
                .processor(masterGenreProcessor())
                .writer(masterGenreWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<MasterGenre> masterGenreReader() {
        ConcurrentLinkedQueue<MasterGenre> queue =
                relationsHolder.pullObjectRelationsQueue(MasterGenre.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemProcessor<MasterGenre, MasterGenre> masterGenreProcessor() throws Exception {
        ItemProcessor<MasterGenre, MasterGenre> syncProcessor = item -> {
            Long masterId = item.getMaster().getId();
            String genreStr = item.getGenre().getName();
            if (masterId == null || genreStr == null || !masterRepository.existsById(masterId)) {
                return null;
            }
            if (!genresCache.containsKey(genreStr)) {
                Genre entity = genreRepository.save(item.getGenre());
                genresCache.put(entity.getName(), entity.getId());
            }
            MasterGenre masterGenre = new MasterGenre();
            masterGenre.setGenre(genreRepository.getOne(genresCache.get(genreStr)));
            masterGenre.setMaster(masterRepository.getOne(masterId));
            return masterGenre;
        };

        AsyncItemProcessor<MasterGenre, MasterGenre> masterGenreProcessor = new AsyncItemProcessor<>();
        masterGenreProcessor.setDelegate(syncProcessor);
        masterGenreProcessor.setTaskExecutor(taskExecutor);
        masterGenreProcessor.afterPropertiesSet();
        return masterGenreProcessor;
    }

    @Bean
    public AsyncItemWriter<MasterGenre> masterGenreWriter() throws Exception {
        RepositoryItemWriter<MasterGenre> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(masterGenreRepository);
        syncWriter.afterPropertiesSet();
        AsyncItemWriter<MasterGenre> masterGenreWriter = new AsyncItemWriter<>();
        masterGenreWriter.setDelegate(syncWriter);
        masterGenreWriter.afterPropertiesSet();
        return masterGenreWriter;
    }
}
