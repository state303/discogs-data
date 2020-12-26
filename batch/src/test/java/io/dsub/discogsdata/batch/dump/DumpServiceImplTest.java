package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.internal.verification.MockAwareVerificationMode;
import org.springframework.data.domain.Example;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class DumpServiceImplTest {

    @Mock
    DumpRepository dumpRepository;

    @Mock
    DumpFetcher dumpFetcher;

    @InjectMocks
    DumpServiceImpl dumpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDumpByEtag() {
        String etagString = "xxxx";
        DiscogsDump dump = DiscogsDump.builder().etag(etagString).build();
        when(dumpRepository.existsByEtag(etagString))
                .thenReturn(true);
        when(dumpRepository.findByEtag(etagString))
                .thenReturn(dump);

        assertThat(dumpService.getDumpByEtag(etagString))
                .isEqualTo(dump);

        assertThrows(DumpNotFoundException.class, () -> dumpService.getDumpByEtag("random"), "dump with etag random not found");
    }

    @Test
    void getMostRecentDumpByType() {
        given(dumpRepository.findTopByDumpTypeOrderByIdDesc(DumpType.ARTIST))
                .willReturn(null);
    }

    @Test
    void getLatestCompletedDumpSet() {
        given(dumpRepository.saveAll(any()))
                .willReturn(null);

        DiscogsDump dump = new DiscogsDump();

        given(dumpRepository.findAllByLastModifiedIsBetween(any(), any()))
                .willReturn(List.of(dump, dump, dump, dump));

        List<DiscogsDump> list = dumpService.getLatestCompletedDumpSet();
        list.forEach(item -> assertEquals(item, dump));

        verify(dumpRepository)
                .findAllByLastModifiedIsBetween(any(), any());
    }

    @Test
    void getDumpListInRange() {
        OffsetDateTime start = OffsetDateTime.MIN;
        OffsetDateTime end = OffsetDateTime.MAX;
        List<DiscogsDump> list = List.of(new DiscogsDump());
        given(dumpRepository.findAllByLastModifiedIsBetween(start, end))
                .willReturn(list);

        assertEquals(list, dumpService.getDumpListInRange(start, end));
        verify(dumpRepository).findAllByLastModifiedIsBetween(start, end);
    }

    @Test
    void getDumpByDumpTypeInRange() {
        OffsetDateTime start = OffsetDateTime.MIN;
        OffsetDateTime end = OffsetDateTime.MAX;
        DiscogsDump artistDump = new DiscogsDump();
        given(dumpRepository.findByDumpTypeAndLastModifiedIsBetween(DumpType.ARTIST, start, end))
                .willReturn(artistDump);
        assertEquals(artistDump, dumpService.getDumpByDumpTypeInRange(DumpType.ARTIST, start, end));
        verify(dumpRepository).findByDumpTypeAndLastModifiedIsBetween(DumpType.ARTIST, start, end);
    }

    @Test
    void getDumpListInYearMonth() {
        List<DiscogsDump> dumpList = List.of(new DiscogsDump());
        given(dumpRepository.findAllByLastModifiedIsBetween(any(), any()))
                .willReturn(dumpList);
        assertEquals(dumpList, dumpService.getDumpListInYearMonth(1992, 5));
        verify(dumpRepository).findAllByLastModifiedIsBetween(any(), any());
    }

    @Test
    void updateDumps() {
        DiscogsDump dump = new DiscogsDump();
        List<DiscogsDump> dummy = List.of(dump, dump, dump, dump);
        given(dumpFetcher.getDiscogsDumps())
                .willReturn(dummy);
        given(dumpRepository.count())
                .willReturn(4L);
        dumpService.updateDumps();
        verify(dumpFetcher).getDiscogsDumps();
        verify(dumpRepository).count();

        dumpService = new DumpServiceImpl(dumpRepository, dumpFetcher);

        dump.setLastModified(OffsetDateTime.now());
        dummy = List.of(dump, dump, dump, dump, dump);
        given(dumpFetcher.getDiscogsDumps())
                .willReturn(dummy);
        given(dumpRepository.count())
                .willReturn(0L);
        dumpService.updateDumps();
        verify(dumpRepository).saveAll(any());
    }

    @Test
    void getAllDumps() {
        DiscogsDump dump = new DiscogsDump();
        given(dumpRepository.saveAll(any()))
                .willReturn(null);
        given(dumpRepository.count()).willReturn(4L);
        given(dumpFetcher.getDiscogsDumps())
                .willReturn(List.of(dump, dump, dump, dump));
        dumpService.getAllDumps();

        verify(dumpRepository).count();
        verify(dumpRepository).findAll();
    }

    @Test
    void isExistsByEtag() {
        given(dumpRepository.existsByEtag(any()))
                .willReturn(false);

        assertFalse(dumpService.isExistsByEtag("testString"));
        verify(dumpRepository)
                .existsByEtag("testString");

        given(dumpRepository.existsByEtag("testString"))
                .willReturn(true);
        assertTrue(dumpService.isExistsByEtag("testString"));
    }
}