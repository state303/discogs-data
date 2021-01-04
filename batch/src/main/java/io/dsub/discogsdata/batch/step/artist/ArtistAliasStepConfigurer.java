package io.dsub.discogsdata.batch.step.artist;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import io.dsub.discogsdata.common.repository.artist.ArtistAliasRepository;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class ArtistAliasStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final DumpCache dumpCache;
    private final ArtistAliasRepository artistAliasRepository;
    private final ArtistRepository artistRepository;

    @Bean
    @JobScope
    public Step artistAliasStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory
                .get("artistAliasStep " + etag)
                .<SimpleRelation, Future<ArtistAlias>>chunk(chunkSize)
                .reader(artistAliasItemReader())
                .processor(artistAliasProcessor())
                .writer(artistAliasWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> artistAliasItemReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue = dumpCache.pullSimpleRelationsQueue(XmlArtist.Alias.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemWriter<ArtistAlias> artistAliasWriter() throws Exception {
        RepositoryItemWriter<ArtistAlias> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(artistAliasRepository);
        syncWriter.afterPropertiesSet();
        AsyncItemWriter<ArtistAlias> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(syncWriter);
        asyncWriter.afterPropertiesSet();
        return asyncWriter;
    }

    @Bean
    public AsyncItemProcessor<SimpleRelation, ArtistAlias> artistAliasProcessor() throws Exception {
        AsyncItemProcessor<SimpleRelation, ArtistAlias> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(simpleRelation -> {
            if (!isValidRelation(simpleRelation, artistRepository)) {
                return null;
            }

            Artist artist = Artist.builder().id((Long) simpleRelation.getParent()).build();
            Artist alias = Artist.builder().id((Long) simpleRelation.getChild()).build();

            if (artistAliasRepository.existsByArtistAndAlias(artist, alias)) {
                return null;
            }

            return ArtistAlias.builder()
                    .artist(artist)
                    .alias(alias)
                    .build();
        });
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    private boolean isValidRelation(SimpleRelation simpleRelation, CrudRepository<?, Long> repository) {
        return simpleRelation.getParent() != null &&
                simpleRelation.getChild() != null &&
                repository.existsById((Long) simpleRelation.getParent()) &&
                repository.existsById((Long) simpleRelation.getChild());
    }
}
