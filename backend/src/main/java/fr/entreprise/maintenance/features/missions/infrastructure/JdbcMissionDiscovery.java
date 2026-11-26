package fr.entreprise.maintenance.features.missions.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import fr.entreprise.maintenance.features.missions.domain.DiscoveredMission;
import fr.entreprise.maintenance.features.missions.domain.MissionDiscovery;
import fr.entreprise.maintenance.features.missions.domain.MissionDiscoveryQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMissionDiscovery implements MissionDiscovery {
    private static final String SQL="""
            WITH origin AS (SELECT ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography AS point),
            located AS (
                SELECT m.mission_id, m.title, m.public_summary, m.window_start, m.window_end,
                       m.public_latitude, m.public_longitude, m.public_label,
                       round(ST_Distance(m.public_location, o.point))::bigint AS distance_meters,
                       f.revision_number, f.remuneration, f.expenses, f.currency, f.tax_basis,
                       f.remuneration_basis, f.expenses_basis, sk.skills
                FROM app.mission m
                CROSS JOIN origin o
                JOIN app.mission_financial_revision f ON f.account_id=m.account_id AND f.mission_id=m.mission_id
                    AND f.revision_number=m.financial_revision
                LEFT JOIN LATERAL (
                    SELECT coalesce(array_agg(s.skill ORDER BY s.skill_order),'{}'::varchar[]) AS skills
                    FROM app.mission_skill s WHERE s.account_id=m.account_id AND s.mission_id=m.mission_id
                ) sk ON true
                WHERE m.state='PUBLISHED'
                  AND m.window_end > ?
                  AND ST_DWithin(m.public_location, o.point, ?)
                  AND (?::timestamptz IS NULL OR m.window_start < ?)
                  AND (cardinality(?::text[]) = 0 OR EXISTS (
                      SELECT 1 FROM app.mission_skill s2
                      WHERE s2.account_id=m.account_id AND s2.mission_id=m.mission_id
                        AND lower(s2.skill)=ANY(?::text[])))
            ),
            page AS (
                SELECT * FROM located
                WHERE ?::bigint IS NULL OR (distance_meters, mission_id) > (?::bigint, ?::uuid)
                ORDER BY distance_meters, mission_id
                LIMIT ?
            )
            SELECT * FROM page ORDER BY distance_meters, mission_id
            """;
    private final JdbcTemplate jdbc;
    public JdbcMissionDiscovery(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public List<DiscoveredMission> nearby(MissionDiscoveryQuery query, Instant now) {
        Instant from=query.from()==null || query.from().isBefore(now) ? now : query.from();
        Timestamp to=query.to()==null?null:Timestamp.from(query.to());
        String[] skills=query.skills().stream().map(value -> value.toLowerCase(Locale.ROOT)).toArray(String[]::new);
        return jdbc.query(SQL,JdbcMissionDiscovery::mission,query.longitude(),query.latitude(),Timestamp.from(from),query.radiusMeters(),
                to,to,skills,skills,query.afterDistanceMeters(),query.afterDistanceMeters(),
                query.afterMissionId(),query.size()+1);
    }
    private static DiscoveredMission mission(ResultSet r,int index) throws SQLException {
        var skills=r.getArray("skills");
        return new DiscoveredMission(r.getObject("mission_id",UUID.class),r.getString("title"),r.getString("public_summary"),
                skills==null?List.of():Arrays.stream((String[])skills.getArray()).toList(),
                r.getTimestamp("window_start").toInstant(),r.getTimestamp("window_end").toInstant(),r.getBigDecimal("public_latitude"),
                r.getBigDecimal("public_longitude"),r.getString("public_label"),r.getLong("distance_meters"),
                r.getLong("revision_number"),r.getBigDecimal("remuneration"),r.getBigDecimal("expenses"),r.getString("currency"),
                r.getString("tax_basis"),r.getString("remuneration_basis"),r.getString("expenses_basis"));
    }
}
