package io.dsub.discogsdata.batch.step;

import io.dsub.discogsdata.batch.process.SimpleRelation;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@RequiredArgsConstructor
public class SimpleRelationsBatchPss implements BatchPreparedStatementSetter {

    private final List<SimpleRelation> simpleRelations;

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        ps.setLong(1, (Long) simpleRelations.get(i).getParent());
        ps.setLong(2, (Long) simpleRelations.get(i).getChild());
    }

    /**
     * Return the size of the batch.
     *
     * @return the number of statements in the batch
     */
    @Override
    public int getBatchSize() {
        return simpleRelations.size();
    }
}
