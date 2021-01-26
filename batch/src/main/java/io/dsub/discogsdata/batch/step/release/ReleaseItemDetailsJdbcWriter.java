package io.dsub.discogsdata.batch.step.release;

import io.dsub.discogsdata.batch.xml.object.XmlReleaseItemDetails;
import io.dsub.discogsdata.common.entity.release.ReleaseArtist;
import io.dsub.discogsdata.common.entity.release.ReleaseCreditedArtist;
import io.dsub.discogsdata.common.repository.release.ReleaseArtistRepository;
import io.dsub.discogsdata.common.repository.release.ReleaseCreditedArtistRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReleaseItemDetailsJdbcWriter implements ItemWriter<XmlReleaseItemDetails> {

    private final ReleaseArtistRepository releaseArtistRepository;
    private final ReleaseCreditedArtistRepository releaseCreditedArtistRepository;
    private final JdbcTemplate jdbcTemplate;

    @Getter
    @Builder
    private static class Relation {
        private final Long parentId;
        private final Long childId;
        private final String info;
    }

    private static final String RELEASE_ARTIST_INSERT_SQL =
            "INSERT INTO release_artist(release_item_id, artist_id) " +
                    "SELECT ?,? FROM DUAL " +
                    "WHERE NOT EXISTS(SELECT * FROM release_artist WHERE release_item_id = ? AND artist_id = ?)";

    private static final String RELEASE_CREDITED_ARTIST_INSERT_SQL =
            "INSERT INTO release_credited_artist(artist_id, release_item_id, role) " +
                    "SELECT ?, ?, ? FROM DUAL " +
                    "WHERE NOT EXISTS(SELECT * FROM release_credited_artist WHERE artist_id = ? AND release_item_id = ?)";

    private static final String RELEASE_CREDITED_ARTIST_UPDATE_SQL =
            "UPDATE release_credited_artist SET role = ? WHERE artist_id = ? AND release_item_id = ?";

    private static final String RELEASE_ARTIST_REMOVE_SQL =
            "DELETE FROM release_artist WHERE id = ?";

    private static final String RELEASE_CREDITED_ARTIST_REMOVE_SQL =
            "DELETE FROM release_credited_artist WHERE id = ?";

    /*

    INSERT PROCEDURE

    A. >> IF EXISTS IN DB BUT __NOT__ IN XML >> REMOVE ENTRY
    B. >> CHECK IF ENTRY EXISTS >> Y > CHECK IF A / B SAME >> Y > IGNORE ? > N > UPDATE
    C. \n >> N > INSERT


    1. RELEASE_ARTIST

     */

    @Override
    public void write(List<? extends XmlReleaseItemDetails> items) {
        writeReleaseArtists(items);

    }


    ////////////////////////////////////////////////////////////
    // RELEASE_ARTIST
    ////////////////////////////////////////////////////////////

    private void writeReleaseArtists(List<? extends XmlReleaseItemDetails> items) {

        Map<Long, List<Long>> xmlEntryMap = new HashMap<>();

        List<Relation> entriesToPersist = items.stream()
                .map(release -> release.getReleaseArtists().stream()
                        .map(releaseArtist -> Relation.builder()
                                .parentId(release.getReleaseId())
                                .childId(releaseArtist.getId())
                                .build())
                        .collect(Collectors.toList()))
                .reduce(this::concat).orElse(new ArrayList<>());

        for (Relation relation : entriesToPersist) {
            List<Long> entries = xmlEntryMap.getOrDefault(relation.getParentId(), new ArrayList<>());
            entries.add(relation.getChildId());
            xmlEntryMap.put(relation.getParentId(), entries);
        }

        List<ReleaseArtist> previouslyPersistedEntries = items.stream()
                .map(release -> releaseArtistRepository.findAllByReleaseItemId(release.getReleaseId()))
                .reduce(this::concat)
                .orElse(new ArrayList<>())
                .stream()
                .filter(item -> !xmlEntryMap.containsKey(item.getReleaseItem().getId()) ||
                        !xmlEntryMap.get(item.getReleaseItem().getId()).contains(item.getArtist().getId()))
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(RELEASE_ARTIST_REMOVE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, previouslyPersistedEntries.get(i).getId());
            }

            @Override
            public int getBatchSize() {
                return previouslyPersistedEntries.size();
            }
        });

        jdbcTemplate.batchUpdate(RELEASE_ARTIST_INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, entriesToPersist.get(i).getParentId());
                ps.setLong(2, entriesToPersist.get(i).getChildId());
                ps.setLong(3, entriesToPersist.get(i).getParentId());
                ps.setLong(4, entriesToPersist.get(i).getChildId());
            }

            @Override
            public int getBatchSize() {
                return entriesToPersist.size();
            }
        });
    }

    private void writeReleaseCreditedArtists(List<? extends XmlReleaseItemDetails> items) {
        List<Relation> creditedArtistsToPersist = items.stream()
                .map(release -> release.getCreditedArtists().stream()
                        .map(creditedArtist -> Relation.builder()
                                .parentId(release.getReleaseId())
                                .childId(creditedArtist.getId())
                                .info(creditedArtist.getRole())
                                .build())
                        .collect(Collectors.toList()))
                .reduce(this::concat).orElse(new ArrayList<>());

        List<ReleaseCreditedArtist> creditedArtistsToDrop = items.stream()
                .map(release -> releaseCreditedArtistRepository.findAllByReleaseItemId(release.getReleaseId()))
                .reduce(this::concat).orElse(new ArrayList<>())
                .stream()
                .filter(item -> creditedArtistsToPersist.contains(Relation.builder()
                        .parentId(item.getReleaseItem().getId())
                        .childId(item.getArtist().getId())
                        .build()))
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(RELEASE_CREDITED_ARTIST_REMOVE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, creditedArtistsToDrop.get(i).getId());
            }

            @Override
            public int getBatchSize() {
                return creditedArtistsToDrop.size();
            }
        });

        Map<Boolean, List<Relation>> entriesToBeInserted = creditedArtistsToPersist.stream()
                .collect(Collectors.groupingBy(relation -> releaseCreditedArtistRepository.existsByArtistIdAndReleaseItemId(relation.getChildId(), relation.getParentId())));

        jdbcTemplate.batchUpdate(RELEASE_CREDITED_ARTIST_UPDATE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, entriesToBeInserted.get(true).get(i).getInfo());
                ps.setLong(2, entriesToBeInserted.get(true).get(i).getChildId());
                ps.setLong(3, entriesToBeInserted.get(true).get(i).getParentId());
            }

            @Override
            public int getBatchSize() {
                return entriesToBeInserted.get(true).size();
            }
        });

        jdbcTemplate.batchUpdate(RELEASE_CREDITED_ARTIST_INSERT_SQL, new BatchPreparedStatementSetter() {

            List<Relation> targetList = entriesToBeInserted.get(false);

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Relation target = targetList.get(i);
                ps.setLong(1, target.getChildId());
                ps.setLong(2, target.getParentId());
                ps.setString(3, target.getInfo());
                ps.setLong(4, target.getChildId());
                ps.setLong(5, target.getParentId());
            }

            @Override
            public int getBatchSize() {
                return targetList.size();
            }
        });
    }

    private <T> List<T> concat(List<T> left, List<T> right) {
        left.addAll(right);
        return left;
    }

}
