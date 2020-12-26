package io.dsub.discogsdata.batch.step;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class FileCopyTasklet implements Tasklet {

    private final DiscogsDump dump;

    public FileCopyTasklet(DiscogsDump dump) {
        this.dump = dump;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Path fileName = Path.of(dump.getUri().split("/")[2]);
        if (Files.exists(fileName)) {
            log.info("found duplicated file: {}. checking size...", fileName);
            if (Files.size(fileName) == dump.getSize()) {
                log.info("file already exists. proceeding...");
                return RepeatStatus.FINISHED;
            }
            Files.delete(fileName);
            log.info("incomplete size. deleted previous {}.", fileName);
        }
        try (InputStream in = new URL(dump.getResourceUrl()).openStream()) {
            ProgressBarBuilder pbb = new ProgressBarBuilder()
                    .setTaskName("fetching " + dump.getUri().split("/")[2])
                    .setUnit("MB", 1048576)
                    .setInitialMax(dump.getSize())
                    .showSpeed();
            Files.copy(ProgressBar.wrap(in, pbb), fileName);
        }
        log.info("successfully fetched {}.", fileName);
        return RepeatStatus.FINISHED;
    }

}
