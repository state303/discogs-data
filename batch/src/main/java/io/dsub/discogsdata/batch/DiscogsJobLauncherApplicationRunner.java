package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.batch.util.DiscogsJobParameterResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class DiscogsJobLauncherApplicationRunner extends JobLauncherApplicationRunner {
    /**
     * Create a new {@link JobLauncherApplicationRunner}.
     *
     * @param jobLauncher   to launch jobs
     * @param jobExplorer   to check the job repository for previous executions
     * @param jobRepository to check if a job instance exists with the given parameters
     */

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
    private final DiscogsJobParameterResolver parameterResolver;

    public DiscogsJobLauncherApplicationRunner(JobLauncher jobLauncher, JobExplorer jobExplorer, JobRepository jobRepository, DiscogsJobParameterResolver parameterResolver) {
        super(jobLauncher, jobExplorer, jobRepository);
        this.jobExplorer = jobExplorer;
        this.jobLauncher = jobLauncher;
        this.jobRepository = jobRepository;
        this.parameterResolver = parameterResolver;
    }

    @Override
    public void execute(Job job, JobParameters jobParameters) throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        JobParameters parameters = getNextJobParameters(job, jobParameters);
        parameters = parameterResolver.resolve(parameters);
        log.debug("executing batch process with resolved parameters: {}", parameters);
        JobExecution execution = this.jobLauncher.run(job, parameters);
        log.debug("jobInstance id: {}", execution.getJobInstance());
    }

    /*
     * Identical copies of methods from JobLauncherApplicationRunner.class, as the access modifiers are set to private.
     * */

    private JobParameters getNextJobParameters(Job job, JobParameters jobParameters) {
        if (this.jobRepository != null && this.jobRepository.isJobInstanceExists(job.getName(), jobParameters)) {
            return getNextJobParametersForExisting(job, jobParameters);
        }
        if (job.getJobParametersIncrementer() == null) {
            return jobParameters;
        }

        JobParameters nextParameters = new JobParametersBuilder(jobParameters, this.jobExplorer)
                .getNextJobParameters(job)
                .toJobParameters();

        return merge(nextParameters, jobParameters);
    }

    private JobParameters getNextJobParametersForExisting(Job job, JobParameters jobParameters) {
        JobExecution lastExecution = this.jobRepository.getLastJobExecution(job.getName(), jobParameters);
        if (isStoppedOrFailed(lastExecution) && job.isRestartable()) {
            JobParameters previousIdentifyingParameters = getGetIdentifying(lastExecution.getJobParameters());
            return merge(previousIdentifyingParameters, jobParameters);
        }
        return jobParameters;
    }

    private boolean isStoppedOrFailed(JobExecution execution) {
        BatchStatus status = (execution != null) ? execution.getStatus() : null;
        return (status == BatchStatus.STOPPED || status == BatchStatus.FAILED);
    }

    private JobParameters getGetIdentifying(JobParameters parameters) {
        HashMap<String, JobParameter> nonIdentifying = new LinkedHashMap<>(parameters.getParameters().size());
        parameters.getParameters().forEach((key, value) -> {
            if (value.isIdentifying()) {
                nonIdentifying.put(key, value);
            }
        });
        return new JobParameters(nonIdentifying);
    }

    private JobParameters merge(JobParameters parameters, JobParameters additions) {
        Map<String, JobParameter> merged = new LinkedHashMap<>();
        merged.putAll(parameters.getParameters());
        merged.putAll(additions.getParameters());
        return new JobParameters(merged);
    }
}
