package io.dsub.discogsdata.batch.process;

import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.time.LocalDate;
import java.util.List;

public interface JobService {
    Long launchByEtag(List<String> etagList) throws JobParametersInvalidException, JobInstanceAlreadyExistsException, NoSuchJobException, DumpNotFoundException;

    Long launchRecentJob() throws JobParametersInvalidException, JobInstanceAlreadyExistsException, NoSuchJobException;

    Long launchWithTypeList(List<String> typeList);

    Long launchWithTypeAndYearMonthDate(String type, LocalDate localDate);

    Long launchAllByYearMonthDate(LocalDate localDate);

    Long launchRecentByType(String type) throws JobInstanceAlreadyCompleteException, JobInstanceAlreadyExistsException;

    Long restartJobById(Long id) throws JobInstanceAlreadyCompleteException, JobParametersInvalidException, JobRestartException, NoSuchJobExecutionException, NoSuchJobException;

    JobExecution findJobByExecutionId(Long id) throws NoSuchJobExecutionException;

    boolean stopExecutionById(Long id) throws NoSuchJobExecutionException, JobExecutionNotRunningException;
}