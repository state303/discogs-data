package io.dsub.discogsdata.batch.step;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
public class FileCleanupTasklet implements Tasklet {

    private final DiscogsDump dump;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Files.deleteIfExists(Path.of(dump.getUri().split("/")[2]));
        return RepeatStatus.FINISHED;
    }
}
