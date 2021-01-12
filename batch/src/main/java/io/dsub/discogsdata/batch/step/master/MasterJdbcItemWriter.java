package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.step.SimpleRelationsBatchPss;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import io.dsub.discogsdata.common.repository.GenreRepository;
import io.dsub.discogsdata.common.repository.StyleRepository;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import io.dsub.discogsdata.common.repository.master.MasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class MasterJdbcItemWriter implements ItemWriter<XmlMaster> {

    private static final String INSERT_MASTER_SQL =
            "INSERT INTO master (id, created_at, last_modified_at, data_quality, title, year, main_release_item_id) " +
                    "VALUES (?, NOW(), NOW(), ?,?,?,?)";

    private static final String UPDATE_MASTER_SQL =
            "UPDATE master SET last_modified_at = NOW(), " +
                    "title = ?, " +
                    "year = ?, " +
                    "data_quality = ?, " +
                    "main_release_item_id = ? " +
                    "WHERE id = ?";

    private static final String INSERT_MASTER_VIDEO_SQL =
            "INSERT INTO master_video(description, title, url, master_id) VALUES (?,?,?,?)";

    private final JdbcTemplate jdbcTemplate;
    private final MasterRepository masterRepository;

    private final GenreRepository genreRepository;
    private final StyleRepository styleRepository;
    private final ArtistRepository artistRepository;

    @Override
    public void write(List<? extends XmlMaster> items) {
        Map<Boolean, List<XmlMaster>> listMap = items.stream()
                .collect(Collectors.groupingByConcurrent(item -> masterRepository.existsById(item.getId())));
        List<XmlMaster> mastersToUpdate = listMap.get(true);
        List<XmlMaster> mastersToInsert = listMap.get(false);

        if (mastersToInsert != null && mastersToInsert.size() > 0) {
            insertMasters(mastersToInsert);
        }

        if (mastersToUpdate != null && mastersToUpdate.size() > 0) {
            updateMasters(mastersToUpdate);
        }

        pruneMasterVideos(items);

        List<MasterVideo> masterVideos = items.stream()
                .map(item ->
                    item.getVideos().stream()
                            .map(video -> video.toMasterVideo(item.getId()))
                            .collect(Collectors.toList()))
                .reduce((acc, curr) -> {
                    acc.addAll(curr);
                    return acc;
                }).orElse(new ArrayList<>());

        if (masterVideos.size() > 0) {
            insertMasterVideos(masterVideos);
        }

        pruneMasterArtists(items);

        List<SimpleRelation> masterArtists = items.stream()
                .map(item -> item.getArtists().stream()
                        .filter(artistInfo -> artistRepository.existsById(artistInfo.getId()))
                        .map(artistInfo -> new SimpleRelation(item.getId(), artistInfo.getId()))
                        .collect(Collectors.toList()))
                .reduce((acc, curr) -> {
                    acc.addAll(curr);
                    return acc;
                }).orElse(new ArrayList<>());

        insertMasterArtists(masterArtists);

        List<SimpleRelation> masterGenres = items.stream()
                .map(item -> item.getGenres().stream()
                        .map(genreString -> new SimpleRelation(item.getId(), genreString))
                        .collect(Collectors.toList()))
                .reduce((acc, curr) -> {
                    acc.addAll(curr);
                    return acc;
                }).orElse(new ArrayList<>());

        insertMasterGenres(masterGenres);

        List<SimpleRelation> masterStyles = items.stream()
                .map(item -> item.getStyles().stream()
                        .map(styleString -> new SimpleRelation(item.getId(), styleString)).collect(Collectors.toList()))
                .reduce((acc, curr) -> {
                    acc.addAll(curr);
                    return acc;
                }).orElse(new ArrayList<>());

        insertMasterStyles(masterStyles);
    }

    private void insertMasterStyles(List<? extends SimpleRelation> masterStyles) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO master_style(master_id, style_id) SELECT ?, ? FROM DUAL WHERE NOT EXISTS(SELECT * FROM master_style WHERE master_id = ? AND style_id = ?)",
                getStylesGenresBatchPss(masterStyles));
    }

    private void insertMasterGenres(List<? extends SimpleRelation> masterGenres) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO master_genre(master_id, genre_id) SELECT ?, ? FROM DUAL WHERE NOT EXISTS(SELECT * FROM master_genre WHERE master_id = ? AND genre_id = ?)",
                getStylesGenresBatchPss(masterGenres));
    }

    private BatchPreparedStatementSetter getStylesGenresBatchPss(List<? extends SimpleRelation> simpleRelations) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Long genreId = genreRepository.findByName(String.valueOf(simpleRelations.get(i).getChild()));
                ps.setLong(1, (Long) simpleRelations.get(i).getParent());
                ps.setLong(2, genreId);
                ps.setLong(3, (Long) simpleRelations.get(i).getParent());
                ps.setLong(4, genreId);
            }
            @Override
            public int getBatchSize() {
                return simpleRelations.size();
            }
        };
    }

    private void insertMasterArtists(List<SimpleRelation> masterArtists) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO master_artist(artist_id, master_id) VALUES (?, ?)",
                new SimpleRelationsBatchPss(masterArtists));
    }

    private void pruneMasterArtists(List<? extends XmlMaster> masters) {
        jdbcTemplate.batchUpdate("DELETE FROM master_artist WHERE master_id = ?", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, masters.get(i).getId());
            }
            @Override
            public int getBatchSize() {
                return masters.size();
            }
        });
    }

    private void pruneMasterVideos(List<? extends XmlMaster> items) {
        jdbcTemplate.batchUpdate("DELETE FROM master_video WHERE master_id = ?", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, items.get(i).getId());
            }
            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }

    private void insertMasterVideos(List<MasterVideo> masterVideos) {
        jdbcTemplate.batchUpdate(INSERT_MASTER_VIDEO_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, masterVideos.get(i).getDescription());
                ps.setString(2, masterVideos.get(i).getTitle());
                ps.setString(3, masterVideos.get(i).getUrl());
                ps.setLong(4, masterVideos.get(i).getMaster().getId());
            }
            @Override
            public int getBatchSize() {
                return masterVideos.size();
            }
        });
    }

    private void insertMasters(List<XmlMaster> items) {
        jdbcTemplate.batchUpdate(INSERT_MASTER_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, items.get(i).getId());
                ps.setString(2, items.get(i).getDataQuality());
                ps.setString(3, items.get(i).getTitle());
                ps.setShort(4, items.get(i).getYear());
                ps.setLong(5, items.get(i).getMainRelease());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }

    private void updateMasters(List<XmlMaster> masters) {
        jdbcTemplate.batchUpdate(UPDATE_MASTER_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, masters.get(i).getTitle());
                ps.setShort(2, masters.get(i).getYear());
                ps.setString(3, masters.get(i).getDataQuality());
                ps.setLong(4, masters.get(i).getMainRelease());
                ps.setLong(5, masters.get(i).getId());
            }

            @Override
            public int getBatchSize() {
                return masters.size();
            }
        });
    }
}
