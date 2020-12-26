package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.dump.DefaultDumpDependencyResolver;
import io.dsub.discogsdata.batch.dump.DumpDependencyResolver;
import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.DumpServiceImpl;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.BDDAssumptions.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SimpleDiscogsJobParameterResolverTest {

    @InjectMocks
    private SimpleDiscogsJobParameterResolver parameterResolver;
    @Mock
    private DefaultDumpDependencyResolver dependencyResolver;
    @Mock
    private DumpServiceImpl dumpService;

    private DiscogsDump artistDump;
    private DiscogsDump releaseDump;
    private DiscogsDump masterDump;
    private DiscogsDump labelDump;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        artistDump = new DiscogsDump();
        releaseDump = new DiscogsDump();
        masterDump = new DiscogsDump();
        labelDump = new DiscogsDump();

        artistDump.setDumpType(DumpType.ARTIST);
        releaseDump.setDumpType(DumpType.RELEASE);
        masterDump.setDumpType(DumpType.MASTER);
        labelDump.setDumpType(DumpType.LABEL);

        for (DiscogsDump dump : new DiscogsDump[]{artistDump, releaseDump, labelDump, masterDump}) {
            dump.setEtag(dump.getDumpType().name() + "-etag");
        }
    }


    @Test
    void resolveByEtag() {
        List<String> etags = List.of("a", "b", "c");
        when (dependencyResolver.resolveByEtag(etags))
                .thenReturn(List.of(artistDump, labelDump, masterDump));

        JobParameters etagParams = new JobParametersBuilder()
                .addString("etag", "a,b,c")
                .toJobParameters();

        assertThat(parameterResolver.resolve(etagParams).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("master")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveEmptyParameter() {
        List<DiscogsDump> dumpList = new ArrayList<>();
        dumpList.add(artistDump);
        dumpList.add(releaseDump);
        dumpList.add(labelDump);
        dumpList.add(masterDump);

        when(dumpService.getLatestCompletedDumpSet())
                .thenReturn(dumpList);

        JobParameters emptyJobParameters = new JobParameters();
        emptyJobParameters = parameterResolver.resolve(emptyJobParameters);
        assertThat(emptyJobParameters.getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("release")
                .hasFieldOrProperty("master")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveArtistParameter() {
        JobParameters artistTypeParam = new JobParametersBuilder()
                .addString("types", "artist")
                .toJobParameters();

        List<DiscogsDump> artistDumpList = new ArrayList<>();
        artistDumpList.add(artistDump);

        when(dependencyResolver.resolveByType(any()))
                .thenReturn(artistDumpList);

        assertThat(parameterResolver.resolve(artistTypeParam).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveReleaseParameter() {
        JobParameters releaseTypeParam = new JobParametersBuilder()
                .addString("types", "release")
                .toJobParameters();
        List<DiscogsDump> releaseDumpList = Arrays.asList(artistDump, masterDump, releaseDump, labelDump);
        when(dependencyResolver.resolveByType(any())).thenReturn(releaseDumpList);
        assertThat(parameterResolver.resolve(releaseTypeParam).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("release")
                .hasFieldOrProperty("master")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveYearMonthParameter() {
        JobParameters yearMonthParam = new JobParametersBuilder()
                .addString("YEARMONTH", "1992-04")
                .toJobParameters();
        when(dependencyResolver.resolveByYearMonth(anyString()))
                .thenReturn(Arrays.asList(artistDump, masterDump, labelDump, releaseDump));

        assertThat(parameterResolver.resolve(yearMonthParam).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("release")
                .hasFieldOrProperty("master")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveYearMonthAndTypeParameter() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("YEARmONTH", "1992-04")
                .addString("types", "artist,label")
                .toJobParameters();

        List<String> typeParams = Arrays.asList("artist", "label");

        when(dependencyResolver.resolveByTypeAndYearMonth(typeParams, "1992-04"))
                .thenReturn(Arrays.asList(artistDump, labelDump));

        assertThat(parameterResolver.resolve(parameters).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveChunkSizeParameter() {
        JobParameters chunkSizeParam = new JobParametersBuilder()
                .addLong("chunksize", 1000L)
                .toJobParameters();
        assertThat(parameterResolver.resolve(chunkSizeParam).getLong("chunkSize"))
                .isNotNull()
                .isEqualTo(1000L);
    }
}