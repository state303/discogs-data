package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.DumpCache;
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
    private final DumpCache dumpCache;
    private final MasterArtistRepository masterArtistRepository;

    @Bean
    @JobScope
    public Step masterArtistStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterArtistStep " + etag)
                .<SimpleRelation, Future<MasterArtist>>chunk(chunkSize)
                .reader(masterArtistReader())
                .processor(asyncMasterArtistProcessor())
                .writer(asyncMasterArtistWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> masterArtistReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue =
                dumpCache.pullSimpleRelationsQueue(XmlMaster.ArtistInfo.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemProcessor<SimpleRelation, MasterArtist> asyncMasterArtistProcessor() throws Exception {
        ItemProcessor<SimpleRelation, MasterArtist> syncProcessor = item -> {
            if (item.getParent() == null ||
                    item.getChild() == null ||
                    !masterRepository.existsById((Long) item.getParent()) ||
                    !artistRepository.existsById((Long) item.getChild())) {
                return null;
            }

            Master master = Master.builder().id((Long) item.getParent()).build();
            Artist artist = Artist.builder().id((Long) item.getChild()).build();

            if (masterArtistRepository.existsByMasterAndArtist(master, artist)) {
                return null;
            }

            return MasterArtist.builder()
                    .master(master)
                    .artist(artist)
                    .build();
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
