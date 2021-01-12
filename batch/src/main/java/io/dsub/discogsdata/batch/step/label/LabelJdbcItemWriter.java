package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import io.dsub.discogsdata.common.repository.label.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@StepScope
@Component
@RequiredArgsConstructor
public class LabelJdbcItemWriter implements ItemWriter<XmlLabel> {

    private static final String LABEL_INSERT_SQL =
            "INSERT INTO label(id, created_at, last_modified_at, contact_info, data_quality, name, profile) " +
                    "VALUES (?, NOW(), NOW(), ?, ?, ?, ?)";
    private static final String LABEL_UPDATE_SQL =
            "UPDATE label SET last_modified_at = NOW(), name = ?, contact_info = ?, profile = ?, data_quality = ? WHERE id = ?";
    private static final String LABEL_URL_PRUNE_SQL =
            "DELETE FROM label_url WHERE label_id = ?";
    private static final String LABEL_URL_INSERT_SQL =
            "INSERT INTO label_url(label_id, url) VALUES (?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final LabelRepository labelRepository;

    @Override
    public void write(List<? extends XmlLabel> items) {
        Map<Boolean, List<XmlLabel>> groupedLabels = items.parallelStream()
                .collect(Collectors.groupingByConcurrent(item -> labelRepository.existsById(item.getId())));

        List<XmlLabel> labelsToUpdate = groupedLabels.getOrDefault(true, new LinkedList<>());
        List<XmlLabel> labelsToInsert = groupedLabels.getOrDefault(false, new LinkedList<>());

        updateLabels(labelsToUpdate);
        insertLabels(labelsToInsert);

        List<Long> idList = items.parallelStream()
                .map(XmlLabel::getId)
                .collect(Collectors.toList());

        deleteUrls(idList);

        List<SimpleRelation> urlsToInsert = items.stream()
                .filter(item -> item.getUrls().size() > 0)
                .map(item -> item.getUrls().stream()
                        .map(url -> new SimpleRelation(item.getId(), url))
                        .collect(Collectors.toList()))
                .reduce((acc, curr) -> {
                    acc.addAll(curr);
                    return acc;
                })
                .orElse(new LinkedList<>());

        insertUrls(urlsToInsert);
    }

    private void insertUrls(List<SimpleRelation> urlsToInsert) {
        jdbcTemplate.batchUpdate(LABEL_URL_INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Long id = (Long) urlsToInsert.get(i).getParent();
                String url = (String) urlsToInsert.get(i).getChild();
                ps.setLong(1, id);
                ps.setString(2, url);
            }

            @Override
            public int getBatchSize() {
                return urlsToInsert.size();
            }
        });
    }

    private void deleteUrls(List<Long> idList) {
        jdbcTemplate.batchUpdate(LABEL_URL_PRUNE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, idList.get(i));
            }

            @Override
            public int getBatchSize() {
                return idList.size();
            }
        });
    }

    private void insertLabels(List<XmlLabel> items) {
        if (items.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(LABEL_INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                XmlLabel label = items.get(i);
                ps.setLong(1, label.getId());
                ps.setString(2, label.getContactInfo());
                ps.setString(3, label.getDataQuality());
                ps.setString(4, label.getName());
                ps.setString(5, label.getProfile());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }

    private void updateLabels(List<XmlLabel> items) {
        if (items.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(LABEL_UPDATE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                XmlLabel label = items.get(i);
                ps.setString(1, label.getName());
                ps.setString(2, label.getContactInfo());
                ps.setString(3, label.getProfile());
                ps.setString(4, label.getDataQuality());
                ps.setLong(5, label.getId());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }
}
