package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.step.SimpleRelationsBatchPss;
import io.dsub.discogsdata.batch.xml.object.XmlLabelSubLabel;
import io.dsub.discogsdata.common.entity.label.LabelSubLabel;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
public class LabelSubLabelJdbcItemWriter implements ItemWriter<List<SimpleRelation>> {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Process the supplied data element. Will not be called with any null items
     * in normal operation.
     *
     * @param items items to be written
     */
    @Override
    public void write(List<? extends List<SimpleRelation>> items) {
        List<? extends List<SimpleRelation>> filteredList = items.stream()
                .filter(item -> item.size() > 0)
                .collect(Collectors.toList());

        List<SimpleRelation> simpleRelations = items.stream()
                .reduce((acc, curr) -> {
                    acc.addAll(curr);
                     return acc;
                }).orElse(null);
        if (simpleRelations == null) {
            return;
        }

        jdbcTemplate.batchUpdate("DELETE FROM label_sublabel WHERE parent_label_id = ?", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, (Long) filteredList.get(i).get(0).getParent());
            }

            @Override
            public int getBatchSize() {
                return filteredList.size();
            }
        });

        jdbcTemplate.batchUpdate(
                "INSERT INTO label_sublabel(parent_label_id, sub_label_id) VALUES (?, ?)",
                new SimpleRelationsBatchPss(simpleRelations));
    }
}
