package fr.entreprise.maintenance.features.assets;

import java.util.UUID;
import fr.entreprise.maintenance.testinfra.PostgisFixture;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountsAssetsMigrationIT {
    @Test void upgradingLot04DoesNotCreateAccountsOrChangeExistingIdentitySessions() throws Exception {
        try(var db=PostgisFixture.container()) {
            db.start(); PostgisFixture.migration(db).target("11").load().migrate(); UUID user=UUID.randomUUID(),session=UUID.randomUUID();
            try(var c=PostgisFixture.runtime(db)) {
                try(var s=c.prepareStatement("INSERT INTO app.app_user(user_id,status,created_at,updated_at) VALUES (?,'PENDING',now(),now())")) { s.setObject(1,user); s.executeUpdate(); }
                try(var s=c.prepareStatement("INSERT INTO app.auth_session(session_id,user_id,channel,security_version,created_at,expires_at) VALUES (?,?,'WEB',0,now(),now()+interval '8 hours')")) { s.setObject(1,session); s.setObject(2,user); s.executeUpdate(); }
            }
            String before;
            try(var c=PostgisFixture.migrator(db);var s=c.createStatement();var r=s.executeQuery("SELECT string_agg(version||':'||checksum,',' ORDER BY installed_rank) FROM app.flyway_schema_history")) { r.next(); before=r.getString(1); }
            var flyway=PostgisFixture.migration(db).load(); assertThat(flyway.migrate().migrationsExecuted).isEqualTo(5); flyway.validate(); assertThat(flyway.migrate().migrationsExecuted).isZero();
            try(var c=PostgisFixture.migrator(db);var s=c.createStatement();var r=s.executeQuery("SELECT string_agg(version||':'||checksum,',' ORDER BY installed_rank) FROM app.flyway_schema_history WHERE version::int<=11")) { r.next(); assertThat(r.getString(1)).isEqualTo(before); }
            try(var c=PostgisFixture.runtime(db);var s=c.createStatement();var r=s.executeQuery("SELECT (SELECT count(*) FROM app.client_account),(SELECT count(*) FROM app.auth_session WHERE revoked_at IS NULL)")) { r.next(); assertThat(r.getInt(1)).isZero(); assertThat(r.getInt(2)).isEqualTo(1); }
        }
    }
}
