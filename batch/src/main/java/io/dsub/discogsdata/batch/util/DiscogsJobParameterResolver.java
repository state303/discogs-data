package io.dsub.discogsdata.batch.util;

import org.springframework.batch.core.JobParameters;

/**
 * A JobParameter formatter for discogs batch job
 */
public interface DiscogsJobParameterFormatter {
    JobParameters format(JobParameters jobParameters);
}