package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import io.dsub.discogsdata.batch.xml.object.XmlLabelSubLabel;
import io.dsub.discogsdata.common.repository.label.LabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static io.dsub.discogsdata.batch.step.XmlItemProcessor.IGNORE_FLAG;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LabelSubLabelJdbcStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final LabelSubLabelJdbcItemWriter labelSubLabelJdbcItemWriter;
    private final LabelRepository labelRepository;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @JobScope
    public Step labelSubLabelJdbcStep(@Value("#{jobParameters['label']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("labelSubLabelStep " + etag)
                .<XmlLabelSubLabel, Future<List<SimpleRelation>>>chunk(chunkSize)
                .reader(xmlLabelSubLabelJdbcStepReader(null))
                .processor(xmlLabelSubLabelJdbcStepProcessor())
                .writer(xmlLabelSubLabelJdbcStepWriter())
                .build();
    }

    @Bean
    @StepScope
    public ProgressBarStaxEventItemReader<XmlLabelSubLabel> xmlLabelSubLabelJdbcStepReader(@Value("#{jobParameters['label']}") String etag) throws Exception {
        return new ProgressBarStaxEventItemReader<>(XmlLabelSubLabel.class, dumpService.getDumpByEtag(etag));
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<XmlLabelSubLabel, List<SimpleRelation>> xmlLabelSubLabelJdbcStepProcessor() throws Exception {
        ItemProcessor<XmlLabelSubLabel, List<SimpleRelation>> subLabelListItemProcessor = item -> {
            if (item.getProfile() != null && item.getProfile().contains(IGNORE_FLAG)) {
                return null;
            }
            return item.getSubLabels().stream()
                    .map(subLabel -> new SimpleRelation(item.getId(), subLabel.getId()))
                    .filter(rel -> labelRepository.existsById((Long) rel.getParent()) &&
                            labelRepository.existsById((Long) rel.getChild()))
                    .collect(Collectors.toList());
        };
        AsyncItemProcessor<XmlLabelSubLabel, List<SimpleRelation>> xmlLabelSubLabelListAsyncItemProcessor = new AsyncItemProcessor<>();
        xmlLabelSubLabelListAsyncItemProcessor.setTaskExecutor(taskExecutor);
        xmlLabelSubLabelListAsyncItemProcessor.setDelegate(subLabelListItemProcessor);
        xmlLabelSubLabelListAsyncItemProcessor.afterPropertiesSet();
        return xmlLabelSubLabelListAsyncItemProcessor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<List<SimpleRelation>> xmlLabelSubLabelJdbcStepWriter() throws Exception {
        AsyncItemWriter<List<SimpleRelation>> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(labelSubLabelJdbcItemWriter);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }
}
