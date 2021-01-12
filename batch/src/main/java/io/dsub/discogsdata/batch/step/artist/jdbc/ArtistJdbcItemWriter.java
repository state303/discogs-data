package io.dsub.discogsdata.batch.step.artist.jdbc;

import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
public class ArtistJdbcItemWriter implements ItemWriter<XmlArtist> {

    private final JdbcTemplate jdbcTemplate;
    private final ArtistRepository artistRepository;

    private static final String ARTIST_UPDATE_SQL =
            "UPDATE artist SET last_modified_at = NOW(), data_quality = ?, name = ?, profile = ?, real_name = ? " +
                    "WHERE id=?";

    private static final String ARTIST_INSERT_SQL =
            "INSERT INTO artist(id, created_at, last_modified_at, data_quality, name, profile, real_name) " +
                    "VALUES (?, NOW(), NOW(),?,?,?,?)";

    private static final String ARTIST_NAME_VARIATION_SQL =
            "INSERT INTO artist_name_variation(artist_id, name_variation) VALUES (?, ?)";

    private static final String ARTIST_URL_SQL =
            "INSERT INTO artist_url(artist_id, url) VALUES (?,?)";

    @Override
    public void write(List<? extends XmlArtist> items) {
        Map<Boolean, List<XmlArtist>> grouped = items.parallelStream()
                .collect(Collectors.groupingByConcurrent(
                        xmlArtist -> artistRepository.existsById(xmlArtist.getId())));

        List<XmlArtist> toUpdate = grouped.getOrDefault(true, new ArrayList<>());
        List<XmlArtist> toInsert = grouped.getOrDefault(false, new ArrayList<>());

        artistUpdateBatch(toUpdate);
        artistInsertBatch(toInsert);

        pruneUrlsAndNameVariations(toUpdate);

        artistNameVariationBatch(items);
        artistUrlBatch(items);
    }

    private void pruneUrlsAndNameVariations(List<? extends XmlArtist> xmlArtists) {
        batchPrune(xmlArtists);
    }

    private void batchPrune(List<? extends XmlArtist> xmlArtists) {
        jdbcTemplate.batchUpdate(
                "DELETE FROM artist_name_variation, artist_url " +
                        "USING artist_url " +
                        "JOIN artist_name_variation USING (artist_id) " +
                        "WHERE artist_id = ?", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, xmlArtists.get(i).getId());
            }

            @Override
            public int getBatchSize() {
                return xmlArtists.size();
            }
        });
    }

    private void artistNameVariationBatch(List<? extends XmlArtist> xmlArtists) {
        List<SimpleRelation> simpleRelations = new LinkedList<>();

        for (XmlArtist xmlArtist : xmlArtists) {
            List<SimpleRelation> nameVariations = xmlArtist.getNameVariations().stream()
                    .map(item -> new SimpleRelation(xmlArtist.getId(), item))
                    .collect(Collectors.toList());
            simpleRelations.addAll(nameVariations);
        }

        batchSimpleRelations(simpleRelations, ARTIST_NAME_VARIATION_SQL);
    }

    private void artistUrlBatch(List<? extends XmlArtist> xmlArtists) {
        List<SimpleRelation> simpleRelations = new LinkedList<>();
        for (XmlArtist xmlArtist : xmlArtists) {
            List<SimpleRelation> urls = xmlArtist.getUrls().stream()
                    .map(item -> new SimpleRelation(xmlArtist.getId(), item))
                    .collect(Collectors.toList());
            simpleRelations.addAll(urls);
        }
        batchSimpleRelations(simpleRelations, ARTIST_URL_SQL);
    }

    private void batchSimpleRelations(List<SimpleRelation> simpleRelations, String artistUrlSql) {
        jdbcTemplate.batchUpdate(artistUrlSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SimpleRelation simpleRelation = simpleRelations.get(i);
                ps.setLong(1, (Long) simpleRelation.getParent());
                ps.setString(2, (String) simpleRelation.getChild());
            }
            @Override
            public int getBatchSize() {
                return simpleRelations.size();
            }
        });
    }

    private void artistUpdateBatch(List<XmlArtist> xmlArtists) {
        if (xmlArtists.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(ARTIST_UPDATE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                XmlArtist xmlArtist = xmlArtists.get(i);
                ps.setString(1, xmlArtist.getDataQuality());
                ps.setString(2, xmlArtist.getName());
                ps.setString(3, xmlArtist.getProfile());
                ps.setString(4, xmlArtist.getRealName());
                ps.setLong(5, xmlArtist.getId());
            }

            @Override
            public int getBatchSize() {
                return xmlArtists.size();
            }
        });
    }

    private void artistInsertBatch(List<XmlArtist> xmlArtists) {
        if (xmlArtists.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(ARTIST_INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                XmlArtist xmlArtist = xmlArtists.get(i);
                ps.setLong(1, xmlArtist.getId());
                ps.setString(2, xmlArtist.getDataQuality());
                ps.setString(3, xmlArtist.getName());
                ps.setString(4, xmlArtist.getProfile());
                ps.setString(5, xmlArtist.getRealName());
            }

            @Override
            public int getBatchSize() {
                return xmlArtists.size();
            }
        });
    }
}