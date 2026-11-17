package fr.entreprise.maintenance.shared.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Technical receipts only. Caller must authorize and lock its aggregate BEFORE consulting a receipt. */
@Component
public class CommandReceipts {
    public record Result(long version, String state) { }
    public record ResourceResult(UUID resourceId, long version, String state) { }
    public static final class Rejected extends RuntimeException {
        private final boolean malformed;
        public Rejected(boolean malformed) { super("Command receipt refused."); this.malformed = malformed; }
        public boolean malformed() { return malformed; }
    }
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public CommandReceipts(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Result> replay(UUID actor, String operation, String key, String request) {
        validate(key);
        var rows = jdbc.query("SELECT request_digest,result_version,result_state,expires_at FROM app.command_receipt "
                        + "WHERE actor_user_id=? AND operation=? AND key_digest=?",
                (row, index) -> {
                    if (!row.getString(1).equals(digest(request)) || !row.getTimestamp(4).toInstant().isAfter(clock.instant())) {
                        throw new Rejected(false);
                    }
                    return new Result(row.getLong(2), row.getString(3));
                }, actor, operation, digest(key));
        return rows.stream().findFirst();
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public Result record(UUID actor, String operation, String key, String request, long version, String state) {
        validate(key);
        jdbc.update("INSERT INTO app.command_receipt(actor_user_id,operation,key_digest,request_digest,result_version,result_state,created_at,expires_at) "
                + "VALUES (?,?,?,?,?,?,?,?)", actor, operation, digest(key), digest(request), version, state,
                Timestamp.from(clock.instant()), Timestamp.from(clock.instant().plusSeconds(86400)));
        return new Result(version, state);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ResourceResult> replayResource(UUID actor,String operation,String key,String request) {
        validate(key);
        return jdbc.query("SELECT result_resource_id,result_version,result_state,request_digest,expires_at FROM app.command_receipt WHERE actor_user_id=? AND operation=? AND key_digest=?",
                (row,index)-> {
                    if(!row.getString(4).equals(digest(request)) || !row.getTimestamp(5).toInstant().isAfter(clock.instant()) || row.getObject(1)==null) { throw new Rejected(false); }
                    return new ResourceResult(row.getObject(1,UUID.class),row.getLong(2),row.getString(3));
                },actor,operation,digest(key)).stream().findFirst();
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public ResourceResult recordResource(UUID actor,String operation,String key,String request,UUID resource,long version,String state) {
        validate(key);
        jdbc.update("INSERT INTO app.command_receipt(actor_user_id,operation,key_digest,request_digest,result_resource_id,result_version,result_state,created_at,expires_at) VALUES (?,?,?,?,?,?,?,?,?)",
                actor,operation,digest(key),digest(request),resource,version,state,Timestamp.from(clock.instant()),Timestamp.from(clock.instant().plusSeconds(86400)));
        return new ResourceResult(resource,version,state);
    }
    private static void validate(String key) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{16,100}")) { throw new Rejected(true); }
    }
    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("Required digest unavailable."); }
    }
}
