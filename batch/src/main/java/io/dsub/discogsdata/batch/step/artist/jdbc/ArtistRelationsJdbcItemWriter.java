package io.dsub.discogsdata.batch.step.artist.jdbc;

import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.step.SimpleRelationsBatchPss;
import io.dsub.discogsdata.batch.xml.object.XmlArtistRelation;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ArtistRelationsJdbcItemWriter implements ItemWriter<XmlArtistRelation> {

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_INTO = "INSERT INTO ";
    private static final String VALUES = "VALUES (?,?)";

    private static final String ARTIST_ALIAS_SQL = INSERT_INTO + "artist_alias(artist_id, alias_id) " + VALUES;
    private static final String ARTIST_GROUP_SQL = INSERT_INTO + "artist_group(artist_id, group_id) " + VALUES;
    private static final String ARTIST_MEMBER_SQL = INSERT_INTO + "artist_member(artist_id, member_id) " + VALUES;

    private static final String ARTIST_REL_PRUNE_SQL = "DELETE FROM artist_alias, artist_group, artist_member " +
            "USING artist_alias " +
            "JOIN artist_group USING (artist_id) " +
            "JOIN artist_member USING (artist_id) " +
            "WHERE artist_id = ?";


    @Override
    public void write(List<? extends XmlArtistRelation> items) {

        List<Long> idList = items.parallelStream()
                .map(XmlArtistRelation::getId)
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(ARTIST_REL_PRUNE_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, idList.get(i));
            }

            @Override
            public int getBatchSize() {
                return idList.size();
            }
        });

        List<SimpleRelation> groups = new LinkedList<>();
        List<SimpleRelation> members = new LinkedList<>();
        List<SimpleRelation> aliases = new LinkedList<>();

        items.stream().map(XmlArtistRelation::toSimpleRelations)
                .forEach(map -> {
                    groups.addAll(map.getOrDefault("groups", new LinkedList<>()));
                    members.addAll(map.getOrDefault("members", new LinkedList<>()));
                    aliases.addAll(map.getOrDefault("aliases", new LinkedList<>()));
                });

        batch(aliases, ARTIST_ALIAS_SQL);
        batch(groups, ARTIST_GROUP_SQL);
        batch(members, ARTIST_MEMBER_SQL);
    }

    private void batch(List<SimpleRelation> data, String sql) {
        if (data.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(sql, new SimpleRelationsBatchPss(data, 1));
    }
}
