//package io.dsub.discogsdata.batch.process;
//
//
//import io.dsub.discogsdata.batch.dump.DumpService;
//import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
//import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
//import io.dsub.discogsdata.common.exception.InvalidRequestParamException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.JobExecution;
//import org.springframework.batch.core.JobParameters;
//import org.springframework.batch.core.JobParametersBuilder;
//import org.springframework.batch.core.JobParametersInvalidException;
//import org.springframework.batch.core.configuration.DuplicateJobException;
//import org.springframework.batch.core.configuration.JobFactory;
//import org.springframework.batch.core.configuration.JobRegistry;
//import org.springframework.batch.core.explore.JobExplorer;
//import org.springframework.batch.core.launch.*;
//import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
//import org.springframework.batch.core.repository.JobRestartException;
//import org.springframework.boot.json.JsonParser;
//import org.springframework.boot.json.JsonParserFactory;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class SimpleJobService implements JobService {
//
//    private final JobOperator jobOperator;
//    private final JobRegistry jobRegistry;
//    private final JobExplorer jobExplorer;
//
//    private final DumpService dumpService;
//    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();
//
//    private final JobExecutionResolver resolver;
//
//    @Override
//    public Long launchByEtag(List<String> etagList) throws JobParametersInvalidException, JobInstanceAlreadyExistsException, NoSuchJobException, DumpNotFoundException {
//
//        // TODO: validator, resolver
//        List<DiscogsDump> dumpList = extractDumps(etagList);
//        dumpList = resolver.resolve(dumpList);
//
//        JobParametersBuilder builder = new JobParametersBuilder();
//
//        dumpList.forEach(dump -> builder.addString(dump.getRootElementName(), dump.getEtag()));
//
//        JobParameters jobParameters = builder.toJobParameters();
//
//        return jobOperator.start("entityJob", jobParameters.toString());
////        return jobOperator.start("artistXmlInitialStep", "etag");
//    }
//
//    private List<DiscogsDump> extractDumps(List<String> etagList) {
//        List<DiscogsDump> dumpList = new ArrayList<>();
//        for (String etag : etagList) {
//            if (!dumpService.isExistsByEtag(etag)) {
//                throw new DumpNotFoundException(etag);
//            }
//            dumpList.add(dumpService.getDumpByEtag(etag));
//        }
//        return dumpList;
//    }
//
//    private void register (JobFactory jobFactory) {
//        if (jobExplorer.getJobNames().contains(jobFactory.getJobName())) {
//            return;
//        }
//        try {
//            jobRegistry.register(jobFactory);
//        } catch (DuplicateJobException ignored) {
//            log.debug("JobRegistry attempted to register Job with duplicated name [{}]", jobFactory.getJobName());
//        }
//    }
//
//    @Override
//    public Long launchRecentJob() throws JobParametersInvalidException, JobInstanceAlreadyExistsException, NoSuchJobException {
//        List<DiscogsDump> recentList = dumpService.getLatestCompletedDumpSet();
//
//        String param = recentList.stream().map(i -> i.getDumpType().name() + "=" + i.getResourceUrl()).collect(Collectors.joining(","));
//
//        jobOperator.start("fullOps", param);
//
////        launchWithEtagList(recentList.stream().map(DiscogsDump::getEtag).collect(Collectors.toList()));
//        return null;
//    }
//
//    private void validateRequestEtagSet(List<DiscogsDump> list) throws InvalidRequestParamException {
//        if (list.size() > 4) {
//            throw new InvalidRequestParamException(
//                    String.format("Request parameter limit exceeded (4, current: %d)", list.size()));
//        }
//        LocalDateTime last = null;
//        for (DiscogsDump discogsDump : list) {
//            LocalDateTime curr = discogsDump.getLastModified();
//            if (last == null) {
//                last = curr;
//                continue;
//            }
//            if (last.getMonth() != curr.getMonth() || last.getYear() != curr.getYear()) {
//                String errString = "Etag list must be published under same month, same year";
//                throw new InvalidRequestParamException(errString);
//            }
//            last = curr;
//        }
//    }
//
//    @Override
//    public Long launchWithTypeList(List<String> typeList) {
//        return null;
//    }
//
//    @Override
//    public Long launchWithTypeAndYearMonthDate(String type, LocalDate localDate) {
//        return null;
//    }
//
//    @Override
//    public Long launchAllByYearMonthDate(LocalDate localDate) {
//        return null;
//    }
//
//    @Override
//    public Long launchRecentByType(String type) {
//        return null;
//    }
//
//    @Override
//    public Long restartJobById(Long id) throws JobParametersInvalidException, JobRestartException, JobInstanceAlreadyCompleteException, NoSuchJobExecutionException, NoSuchJobException {
//        return jobOperator.restart(id);
//    }
//
//    @Override
//    public JobExecution findJobByExecutionId(Long id) throws NoSuchJobExecutionException {
//        JobExecution jobExecution = jobExplorer.getJobExecution(id);
//        if (jobExecution == null) {
//            throw new NoSuchJobExecutionException("JobExecution of id " + id + " does not exist");
//        }
//        return jobExplorer.getJobExecution(id);
//    }
//
//    @Override
//    public boolean stopExecutionById(Long id) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
//        return jobOperator.stop(id);
//    }
//}
