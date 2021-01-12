package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.xml.object.XmlMasterSubData;
import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.repository.GenreRepository;
import io.dsub.discogsdata.common.repository.StyleRepository;
import io.dsub.discogsdata.common.repository.release.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@StepScope
@Component
@RequiredArgsConstructor
public class MasterJdbcPreStepWriter implements ItemWriter<XmlMasterSubData> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void write(List<? extends XmlMasterSubData> items) {

        List<String> genres = new ArrayList<>(items.stream()
                .map(XmlMasterSubData::getGenres)
                .reduce((acc, curr) -> {
                    acc.addAll(curr);
                    return acc;
                })
                .orElse(new HashSet<>()));

        List<String> styles = new ArrayList<>(items.stream()
                .map(XmlMasterSubData::getStyles)
                .reduce((acc, curr) -> {
                    acc.addAll(curr);
                    return acc;
                })
                .orElse(new HashSet<>()));

        Set<Long> releaseItemIds = items.stream()
                .map(XmlMasterSubData::getMainRelease)
                .collect(Collectors.toSet());

        if (genres.size() > 0) {
            writeGenres(genres);
        }
        if (styles.size() > 0) {
            writeStyles(styles);
        }
        if (releaseItemIds.size() > 0) {
            writeReleaseItems(new ArrayList<>(releaseItemIds));
        }
    }

    private void writeGenres(List<String> genres) {
        batchStylesOrGenres("INSERT INTO genre(name) SELECT ? FROM DUAL WHERE NOT EXISTS(SELECT * FROM genre WHERE name = ?)", genres);
    }

    private void writeStyles(List<String> styles) {
        batchStylesOrGenres("INSERT INTO style(name) SELECT ? FROM DUAL WHERE NOT EXISTS(SELECT * FROM style WHERE name = ?)", styles);
    }

    private void batchStylesOrGenres(String sql, List<String> textValues) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, textValues.get(i));
                ps.setString(2, textValues.get(i));
            }

            @Override
            public int getBatchSize() {
                return textValues.size();
            }
        });
    }

    private void writeReleaseItems(List<Long> releaseItemIds) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO release_item(id, is_master, created_at, last_modified_at) " +
                        "SELECT ? , true, NOW(), NOW() FROM DUAL " +
                        "WHERE NOT EXISTS(SELECT * FROM release_item WHERE id = ?)",
                new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, releaseItemIds.get(i));
                ps.setLong(2, releaseItemIds.get(i));
            }

            @Override
            public int getBatchSize() {
                return releaseItemIds.size();
            }
        });
    }
}
