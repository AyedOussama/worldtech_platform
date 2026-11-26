package fr.entreprise.maintenance.features.missions;

import java.util.UUID;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MissionsMigrationIT {
    @Test void upgradingLot06PreservesItsChecksumsAndOldCommandReceiptsWithoutCreatingBusinessData() throws Exception {
        try(var db=PostgisFixture.container()) {
            db.start(); PostgisFixture.migration(db).target("16").load().migrate(); UUID actor=UUID.randomUUID();
            try(var c=PostgisFixture.runtime(db)) {
                try(var s=c.prepareStatement("INSERT INTO app.app_user(user_id,status,created_at,updated_at) VALUES (?,'PENDING',now(),now())")) { s.setObject(1,actor); s.executeUpdate(); }
                try(var s=c.prepareStatement("INSERT INTO app.command_receipt(actor_user_id,operation,key_digest,request_digest,result_version,result_state,created_at,expires_at) VALUES (?,'old-command',repeat('a',64),repeat('b',64),3,'SUBMITTED',now(),now()+interval '1 day')")) { s.setObject(1,actor); s.executeUpdate(); }
            }
            String before;
            try(var c=PostgisFixture.migrator(db);var s=c.createStatement();var r=s.executeQuery("SELECT string_agg(version||':'||checksum,',' ORDER BY installed_rank) FROM app.flyway_schema_history")) { r.next(); before=r.getString(1); }
            var flyway=PostgisFixture.migration(db).load(); assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2); flyway.validate(); assertThat(flyway.migrate().migrationsExecuted).isZero();
            try(var c=PostgisFixture.migrator(db);var s=c.createStatement();var r=s.executeQuery("SELECT string_agg(version||':'||checksum,',' ORDER BY installed_rank) FROM app.flyway_schema_history WHERE version::int<=16")) { r.next(); assertThat(r.getString(1)).isEqualTo(before); }
            try(var c=PostgisFixture.runtime(db);var s=c.createStatement();var r=s.executeQuery("SELECT result_version,result_state,result_resource_id,(SELECT count(*) FROM app.mission),(SELECT count(*) FROM app.mission_skill),(SELECT count(*) FROM app.mission_financial_revision) FROM app.command_receipt")) { r.next(); assertThat(r.getLong(1)).isEqualTo(3); assertThat(r.getString(2)).isEqualTo("SUBMITTED"); assertThat(r.getObject(3)).isNull(); assertThat(r.getInt(4)).isZero(); assertThat(r.getInt(5)).isZero(); assertThat(r.getInt(6)).isZero(); }
        }
    }
}
