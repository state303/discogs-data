package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlLabelSubLabel;
import io.dsub.discogsdata.common.entity.label.Label;
import io.dsub.discogsdata.common.entity.label.LabelSubLabel;
import io.dsub.discogsdata.common.repository.label.LabelRepository;
import io.dsub.discogsdata.common.repository.label.LabelSubLabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SubLabelStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final LabelSubLabelRepository labelSubLabelRepository;
    private final DumpService dumpService;

    @Bean
    @JobScope
    public Step subLabelStep(@Value("#{jobParameters['label']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory
                .get("subLabelStep " + etag)
                .<XmlLabelSubLabel, Future<List<LabelSubLabel>>>chunk(chunkSize)
                .reader(subLabelReader(null))
                .processor(asyncSubLabelProcessor())
                .writer(asyncLabelSubLabelWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ProgressBarStaxEventItemReader<XmlLabelSubLabel> subLabelReader(@Value("#{jobParameters['label']}") String etag) throws Exception {
        return new ProgressBarStaxEventItemReader<>(XmlLabelSubLabel.class, dumpService.getDumpByEtag(etag));
    }

    @Bean
    public AsyncItemProcessor<XmlLabelSubLabel, List<LabelSubLabel>> asyncSubLabelProcessor() throws Exception {
        ItemProcessor<XmlLabelSubLabel, List<LabelSubLabel>> syncProcessor = item -> {
            List<LabelSubLabel> labelSubLabels = item.getSubLabels().stream()
                    .map(subLabel -> LabelSubLabel.builder()
                            .parent(Label.builder().id(item.getId()).build())
                            .subLabel(Label.builder().id(subLabel.getId()).build())
                            .build())
                    .collect(Collectors.toList());
            labelSubLabelRepository.deleteAllByParent(Label.builder().id(item.getId()).build());
            return labelSubLabels;
        };
        AsyncItemProcessor<XmlLabelSubLabel, List<LabelSubLabel>> asyncSubLabelProcessor = new AsyncItemProcessor<>();
        asyncSubLabelProcessor.setTaskExecutor(taskExecutor);
        asyncSubLabelProcessor.setDelegate(syncProcessor);
        asyncSubLabelProcessor.afterPropertiesSet();
        return asyncSubLabelProcessor;
    }

    @Bean
    public AsyncItemWriter<List<LabelSubLabel>> asyncLabelSubLabelWriter() throws Exception {
        ItemWriter<List<LabelSubLabel>> syncWriter = items -> {
            Optional<? extends List<LabelSubLabel>> optionalLabelSubLabels = items.stream()
                    .reduce((acc, curr) -> {
                        acc.addAll(curr);
                        return acc;
                    });
            if (optionalLabelSubLabels.isEmpty()) {
                return;
            }
            List<LabelSubLabel> labelSubLabels = optionalLabelSubLabels.get();
            labelSubLabelRepository.saveAll(labelSubLabels);
        };
        AsyncItemWriter<List<LabelSubLabel>> asyncLabelSubLabelWriter = new AsyncItemWriter<>();
        asyncLabelSubLabelWriter.setDelegate(syncWriter);
        asyncLabelSubLabelWriter.afterPropertiesSet();
        return asyncLabelSubLabelWriter;
    }
}
