package fr.entreprise.maintenance.features.onboarding;

import java.util.UUID;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdmissionMigrationIT {
    @Test void upgradeFromLot03PreservesExistingUsersSessionsAndMigrationChecksums() throws Exception {
        try(var database=PostgisFixture.container()) {
            database.start();
            PostgisFixture.migration(database).target("7").load().migrate();
            UUID user=UUID.randomUUID(); UUID session=UUID.randomUUID();
            String before;
            try(var c=PostgisFixture.runtime(database)) {
                try(var s=c.prepareStatement("INSERT INTO app.app_user(user_id,status,created_at,updated_at) VALUES (?,'PENDING',now(),now())")) { s.setObject(1,user); s.executeUpdate(); }
                try(var s=c.prepareStatement("INSERT INTO app.auth_session(session_id,user_id,channel,security_version,created_at,expires_at) VALUES (?,?,'WEB',0,now(),now()+interval '8 hours')")) { s.setObject(1,session); s.setObject(2,user); s.executeUpdate(); }
            }
            try(var c=PostgisFixture.migrator(database);var s=c.createStatement();var r=s.executeQuery("SELECT string_agg(version||':'||checksum,',' ORDER BY installed_rank) FROM app.flyway_schema_history")) { r.next(); before=r.getString(1); }
            var upgrade=PostgisFixture.migration(database).load();
            assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(9);
            upgrade.validate(); assertThat(upgrade.migrate().migrationsExecuted).isZero();
            try(var c=PostgisFixture.migrator(database);var s=c.createStatement();var r=s.executeQuery("SELECT string_agg(version||':'||checksum,',' ORDER BY installed_rank) FROM app.flyway_schema_history WHERE version::integer<=7")) { r.next(); assertThat(r.getString(1)).isEqualTo(before); }
            try(var c=PostgisFixture.runtime(database);var s=c.prepareStatement("SELECT count(*) FROM app.auth_session WHERE session_id=? AND revoked_at IS NULL AND authenticated_at IS NOT NULL")) {
                s.setObject(1,session); try(var r=s.executeQuery()) { r.next(); assertThat(r.getInt(1)).isEqualTo(1); }
            }
        }
    }
}
