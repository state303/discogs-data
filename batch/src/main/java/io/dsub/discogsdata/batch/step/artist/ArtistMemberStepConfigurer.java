package io.dsub.discogsdata.batch.step.artist;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistMember;
import io.dsub.discogsdata.common.repository.artist.ArtistMemberRepository;
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
public class ArtistMemberStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RelationsHolder relationsHolder;
    private final ArtistRepository artistRepository;
    private final ArtistMemberRepository artistMemberRepository;

    @Bean
    @JobScope
    public Step artistMemberStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory
                .get("artistMemberStep " + etag)
                .<SimpleRelation, Future<ArtistMember>>chunk(chunkSize)
                .reader(artistMemberItemReader())
                .processor(artistMemberProcessor())
                .writer(artistMemberWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> artistMemberItemReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                relationsHolder.pullSimpleRelationsQueue(XmlArtist.Member.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemWriter<ArtistMember> artistMemberWriter() throws Exception {
        RepositoryItemWriter<ArtistMember> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(artistMemberRepository);
        syncWriter.afterPropertiesSet();
        AsyncItemWriter<ArtistMember> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(syncWriter);
        asyncWriter.afterPropertiesSet();
        return asyncWriter;
    }

    @Bean
    public AsyncItemProcessor<SimpleRelation, ArtistMember> artistMemberProcessor() throws Exception {
        AsyncItemProcessor<SimpleRelation, ArtistMember> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(simpleRelation -> {
            if (!isValidRelation(simpleRelation, artistRepository)) {
                return null;
            }

            Artist artist = Artist.builder().id(simpleRelation.getParentId()).build();
            Artist member = Artist.builder().id(simpleRelation.getChildId()).build();

            if (artistMemberRepository.existsByArtistAndMember(artist, member)) {
                return null;
            }

            return ArtistMember.builder()
                    .artist(artist)
                    .member(member)
                    .build();
        });
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
