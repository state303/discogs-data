package io.dsub.discogsdata.batch.step.artist;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import io.dsub.discogsdata.common.repository.artist.ArtistGroupRepository;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArtistGroupStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RelationsHolder relationsHolder;
    private final ArtistRepository artistRepository;
    private final ArtistGroupRepository artistGroupRepository;

    @Bean
    @JobScope
    public Step artistGroupStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory
                .get("artistGroupStep " + etag)
                .<SimpleRelation, Future<ArtistGroup>>chunk(chunkSize)
                .reader(artistGroupItemReader())
                .processor(artistGroupProcessor())
                .writer(artistGroupWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> artistGroupItemReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                relationsHolder.pullSimpleRelationsQueue(XmlArtist.Group.class);
        return queue::poll;
    }

    @Bean
    @StepScope
    public ItemWriter<ArtistGroup> syncItemGroupWriter() {
        return items -> {
            if (!CollectionUtils.isEmpty(items)) {
                artistGroupRepository.saveAll(items);
            }
        };
    }

    @Bean
    @StepScope
    public AsyncItemWriter<ArtistGroup> artistGroupWriter() throws Exception {
        AsyncItemWriter<ArtistGroup> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(syncItemGroupWriter());
        asyncWriter.afterPropertiesSet();
        return asyncWriter;
    }

    @Bean
    @StepScope
    public ItemProcessor<SimpleRelation, ArtistGroup> synArtistGroupProcessor() {
        return simpleRelation -> {
            if (!isValidRelation(simpleRelation, artistRepository)) {
                return null;
            }
            Artist artist = Artist.builder()
                    .id(simpleRelation.getParentId())
                    .build();
            Artist group = Artist.builder()
                    .id(simpleRelation.getChildId())
                    .build();

            if (artistGroupRepository.existsByArtistAndGroup(artist, group)) {
                return null;
            }

            return ArtistGroup.builder()
                    .artist(artist)
                    .group(group)
                    .build();
        };
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<SimpleRelation, ArtistGroup> artistGroupProcessor() throws Exception {
        AsyncItemProcessor<SimpleRelation, ArtistGroup> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(synArtistGroupProcessor());
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    private boolean isValidRelation(SimpleRelation simpleRelation, CrudRepository<?, Long> repository) {
        return simpleRelation.getParentId() != null &&
                simpleRelation.getChildId() != null &&
                repository.existsById(simpleRelation.getParentId()) &&
                repository.existsById(simpleRelation.getChildId());
    }
}
