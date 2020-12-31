package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterStyle;
import io.dsub.discogsdata.common.repository.StyleRepository;
import io.dsub.discogsdata.common.repository.master.MasterRepository;
import io.dsub.discogsdata.common.repository.master.MasterStyleRepository;
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
public class MasterStyleStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RelationsHolder relationsHolder;
    private final MasterStyleRepository masterStyleRepository;
    private final MasterRepository masterRepository;
    private final StyleRepository styleRepository;
    private final Map<String, Long> stylesCache;

    @Bean
    @StepScope
    public Step masterStyleStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterStyleStep " + etag)
                .<MasterStyle, Future<MasterStyle>>chunk(chunkSize)
                .reader(masterStyleReader())
                .processor(masterStyleProcessor())
                .writer(masterStyleWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<MasterStyle> masterStyleReader() {
        ConcurrentLinkedQueue<MasterStyle> queue =
                relationsHolder.pullObjectRelationsQueue(MasterStyle.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemProcessor<MasterStyle, MasterStyle> masterStyleProcessor() throws Exception {
        ItemProcessor<MasterStyle, MasterStyle> syncProcessor = item -> {
            Long masterId = item.getMaster().getId();
            String styleStr = item.getStyle().getName();
            if (masterId == null || styleStr == null || !masterRepository.existsById(masterId)) {
                return null;
            }
            if (!stylesCache.containsKey(styleStr)) {
                Style entity = styleRepository.save(item.getStyle());
                stylesCache.put(entity.getName(), entity.getId());
            }

            Style style = Style.builder().id(stylesCache.get(styleStr)).build();
            Master master = Master.builder().id(masterId).build();

            return MasterStyle.builder()
                    .master(master)
                    .style(style)
                    .build();
        };

        AsyncItemProcessor<MasterStyle, MasterStyle> masterStyleProcessor = new AsyncItemProcessor<>();
        masterStyleProcessor.setDelegate(syncProcessor);
        masterStyleProcessor.setTaskExecutor(taskExecutor);
        masterStyleProcessor.afterPropertiesSet();
        return masterStyleProcessor;
    }

    @Bean
    public AsyncItemWriter<MasterStyle> masterStyleWriter() throws Exception {
        RepositoryItemWriter<MasterStyle> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(masterStyleRepository);
        syncWriter.afterPropertiesSet();
        AsyncItemWriter<MasterStyle> masterStyleWriter = new AsyncItemWriter<>();
        masterStyleWriter.setDelegate(syncWriter);
        masterStyleWriter.afterPropertiesSet();
        return masterStyleWriter;
    }
}
