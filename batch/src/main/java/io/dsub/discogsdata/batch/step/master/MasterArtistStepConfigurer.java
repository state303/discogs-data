package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterArtist;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import io.dsub.discogsdata.common.repository.master.MasterArtistRepository;
import io.dsub.discogsdata.common.repository.master.MasterRepository;
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
public class MasterArtistStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final MasterRepository masterRepository;
    private final ArtistRepository artistRepository;
    private final RelationsHolder relationsHolder;
    private final MasterArtistRepository masterArtistRepository;

    @Bean
    @JobScope
    public Step masterArtistStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterArtistStep " + etag)
                .<SimpleRelation, Future<MasterArtist>>chunk(chunkSize)
                .reader(masterArtistReader())
                .processor(asyncMasterArtistProcessor())
                .writer(asyncMasterArtistWriter())
                .taskExecutor(taskExecutor)
                .throttleLimit(10)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> masterArtistReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                relationsHolder.pullSimpleRelationsQueue(XmlMaster.ArtistInfo.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemProcessor<SimpleRelation, MasterArtist> asyncMasterArtistProcessor() throws Exception {
        ItemProcessor<SimpleRelation, MasterArtist> syncProcessor = item -> {
            if (item.getParentId() != null &&
                    item.getChildId() != null &&
                    masterRepository.existsById(item.getParentId()) &&
                    artistRepository.existsById(item.getChildId())) {

                Master master = masterRepository.getOne(item.getParentId());
                Artist artist = artistRepository.getOne(item.getChildId());

                if (masterArtistRepository.existsByMasterAndArtist(master, artist)) {
                   return null;
                }

                return MasterArtist.builder()
                        .master(master)
                        .artist(artist)
                        .build();
            }
            return null;
        };
        AsyncItemProcessor<SimpleRelation, MasterArtist> asyncMasterArtistProcessor = new AsyncItemProcessor<>();
        asyncMasterArtistProcessor.setTaskExecutor(taskExecutor);
        asyncMasterArtistProcessor.setDelegate(syncProcessor);
        asyncMasterArtistProcessor.afterPropertiesSet();
        return asyncMasterArtistProcessor;
    }

    @Bean
    public AsyncItemWriter<MasterArtist> asyncMasterArtistWriter() throws Exception {
        RepositoryItemWriter<MasterArtist> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(masterArtistRepository);
        syncWriter.afterPropertiesSet();
        AsyncItemWriter<MasterArtist> asyncMasterArtistWriter = new AsyncItemWriter<>();
        asyncMasterArtistWriter.setDelegate(syncWriter);
        asyncMasterArtistWriter.afterPropertiesSet();
        return asyncMasterArtistWriter;
    }
}
