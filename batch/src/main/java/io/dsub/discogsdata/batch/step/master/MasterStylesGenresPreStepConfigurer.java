package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.repository.GenreRepository;
import io.dsub.discogsdata.common.repository.StyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class MasterStylesGenresPreStepConfigurer {

    private final StyleRepository styleRepository;
    private final GenreRepository genreRepository;
    private final DumpCache dumpCache;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    @JobScope
    public Step masterStylesGenresPreStep(@Value("#{jobParameters['master']}") String etag) {
        return stepBuilderFactory.get("masterSubStep " + etag)
                .tasklet((contribution, chunkContext) -> {
                    List<Genre> genres = dumpCache.getGenresNames().stream()
                            .filter(genre -> !genreRepository.existsByName(genre))
                            .map(genre -> Genre.builder().name(genre).build())
                            .collect(Collectors.toList());
                    List<Style> styles = dumpCache.getStylesNames().stream()
                            .filter(style -> !styleRepository.existsByName(style))
                            .map(style -> Style.builder().name(style).build())
                            .collect(Collectors.toList());
                    genreRepository.saveAll(genres);
                    styleRepository.saveAll(styles);

                    genreRepository.findAll().forEach(genre -> dumpCache.putGenre(genre.getName(), genre.getId()));
                    styleRepository.findAll().forEach(style -> dumpCache.putStyle(style.getName(), style.getId()));
                    return RepeatStatus.FINISHED;
                })
                .throttleLimit(1)
                .build();
    }
}
