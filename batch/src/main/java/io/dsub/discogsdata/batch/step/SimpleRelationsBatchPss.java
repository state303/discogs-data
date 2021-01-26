package io.dsub.discogsdata.batch.step;

import io.dsub.discogsdata.batch.process.SimpleRelation;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

import javax.validation.constraints.Min;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@RequiredArgsConstructor
public class SimpleRelationsBatchPss implements BatchPreparedStatementSetter {

    private final List<SimpleRelation> simpleRelations;
    @Min(1)
    private final int repeatCount;

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        for (int j = 0; j < repeatCount; j++) {
            ps.setLong(j * 2 + 1, (Long) simpleRelations.get(i).getParent());
            ps.setLong(j * 2 + 2, (Long) simpleRelations.get(i).getChild());
        }
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
