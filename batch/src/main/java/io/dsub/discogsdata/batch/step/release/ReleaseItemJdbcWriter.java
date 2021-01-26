package io.dsub.discogsdata.batch.step.release;

import io.dsub.discogsdata.batch.util.MalformedDateParser;
import io.dsub.discogsdata.batch.xml.object.XmlReleaseItem;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReleaseItemJdbcWriter implements ItemWriter<XmlReleaseItem> {

    private static final String RELEASE_ITEM_INSERT_SQL =
            "INSERT INTO release_item(id, country, data_quality, is_master, notes, release_date, status, title, master_id, created_at, last_modified_at) " +
                    "SELECT ?,?,?,?,?,?,?,?,?,NOW(),NOW() FROM DUAL WHERE NOT EXISTS(SELECT * FROM release_item WHERE id = ?)";



    private final JdbcTemplate jdbcTemplate;

    @Override
    public void write(List<? extends XmlReleaseItem> items) {

        jdbcTemplate.batchUpdate(RELEASE_ITEM_INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {

                XmlReleaseItem releaseItem = items.get(i).withTrimmedData();
                LocalDate releaseDate = MalformedDateParser.parse(releaseItem.getReleaseDate());

                ps.setLong(1, releaseItem.getReleaseId());
                ps.setString(2, releaseItem.getCountry());
                ps.setString(3, releaseItem.getDataQuality());
                ps.setBoolean(4, releaseItem.getMaster().isMaster());
                ps.setString(5, releaseItem.getNotes());
                ps.setDate(6, Date.valueOf(releaseDate));
                ps.setString(7, releaseItem.getStatus());
                ps.setString(8, releaseItem.getTitle());
                ps.setLong(9, releaseItem.getMaster().getMasterId());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }
}
