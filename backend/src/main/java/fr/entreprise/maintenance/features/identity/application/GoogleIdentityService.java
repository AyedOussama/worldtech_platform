package fr.entreprise.maintenance.features.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.UUID;
import fr.entreprise.maintenance.features.identity.domain.GoogleFlows;
import fr.entreprise.maintenance.features.identity.domain.GoogleSubject;
import fr.entreprise.maintenance.features.identity.domain.IdentityFailure;
import fr.entreprise.maintenance.features.identity.domain.LoginIdentity;
import fr.entreprise.maintenance.features.identity.domain.OpaqueTokens;
import org.springframework.stereotype.Service;

@Service
public class GoogleIdentityService {
    public record Start(UUID transactionId, String launchPath) {
        @Override public String toString() { return "Start[redacted]"; }
    }
    private final GoogleFlows flows;
    private final GooglePolicy policy;
    private final LoginAttempts limits;
    private final Clock clock;
    private final fr.entreprise.maintenance.features.identity.domain.GoogleAdmission admission;
    public GoogleIdentityService(GoogleFlows flows, GooglePolicy policy, LoginAttempts limits, Clock clock,
            fr.entreprise.maintenance.features.identity.domain.GoogleAdmission admission) {
        this.flows = flows; this.policy = policy; this.limits = limits; this.clock = clock;
        this.admission=admission;
    }
    public Start start(GoogleFlows.Channel channel, GoogleFlows.Intention intention, LoginIdentity identity,
            String browser, String challenge, String destination, String peer) {
        return start(channel,intention,identity,browser,challenge,destination,peer,null,null,null);
    }
    public Start registerTechnician(GoogleFlows.Channel channel,String browser,String challenge,String destination,String peer) {
        return start(channel,GoogleFlows.Intention.LOGIN,null,browser,challenge,destination,peer,GoogleFlows.Registration.TECHNICIAN,null,null);
    }
    public Start registerClient(String browser,String invitationToken,String email,String peer) {
        enabled();
        String normalized=fr.entreprise.maintenance.features.identity.domain.EmailAddress.parse(email).normalized();
        limits.onboarding("GOOGLE_CLIENT_REGISTRATION",normalized,peer);
        String fingerprint=OpaqueTokens.fingerprint(invitationToken);
        admission.checkClientInvitation(fingerprint,normalized);
        return start(GoogleFlows.Channel.WEB,GoogleFlows.Intention.LOGIN,null,browser,null,null,peer,GoogleFlows.Registration.CLIENT,normalized,fingerprint);
    }
    private Start start(GoogleFlows.Channel channel, GoogleFlows.Intention intention, LoginIdentity identity,
            String browser, String challenge, String destination, String peer,GoogleFlows.Registration registration,String email,String invitation) {
        enabled(); limits.action("GOOGLE_START", identity == null ? "" : identity.userId().toString(), peer);
        if (intention == GoogleFlows.Intention.LINK && identity == null) { throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
        if (channel == GoogleFlows.Channel.MOBILE) {
            policy.destination(destination);
            if (challenge == null || !challenge.matches("[A-Za-z0-9_-]{43}")) { throw new IdentityFailure(IdentityFailure.Reason.INVALID_INPUT); }
        }
        var now = clock.instant(); UUID id = UUID.randomUUID(); String launch = OpaqueTokens.generate();
        var transaction = new GoogleFlows.Transaction(id, channel, intention,
                intention == GoogleFlows.Intention.LINK ? identity.userId() : null,
                intention == GoogleFlows.Intention.LINK ? identity.sessionId() : null,
                intention == GoogleFlows.Intention.LINK ? identity.securityVersion() : null,
                channel == GoogleFlows.Channel.MOBILE ? challenge : null, channel == GoogleFlows.Channel.MOBILE ? destination : null, now.plusSeconds(300),registration,email,invitation);
        flows.create(transaction, OpaqueTokens.fingerprint(launch), browser == null ? null : OpaqueTokens.fingerprint(browser), now);
        return new Start(id, "/oauth2/authorization/google?launch=" + launch);
    }
    public GoogleFlows.Transaction launch(String launch, String browser) {
        enabled(); return flows.launch(OpaqueTokens.fingerprint(launch), OpaqueTokens.fingerprint(browser), clock.instant());
    }
    public void saveAuthorization(UUID transaction, String state, String encrypted) {
        enabled(); flows.saveAuthorization(transaction, OpaqueTokens.digest(state), encrypted, clock.instant());
    }
    public GoogleFlows.Authorization authorization(String state, String browser, boolean consume) {
        enabled();
        if (state == null || state.length() > 256 || browser == null) { return null; }
        return flows.authorization(OpaqueTokens.digest(state), OpaqueTokens.fingerprint(browser), consume, clock.instant());
    }
    public GoogleFlows.Completion complete(UUID transaction, GoogleSubject subject, String verifiedEmail, LoginIdentity browserIdentity) {
        enabled(); return flows.complete(transaction, subject, verifiedEmail, browserIdentity, clock.instant());
    }
    public GoogleFlows.Exchange exchange(UUID transaction, String code, String verifier, LoginIdentity mobileIdentity, String peer) {
        enabled(); limits.action("GOOGLE_EXCHANGE", "", peer);
        if (verifier == null || !verifier.matches("[A-Za-z0-9._~-]{43,128}")) { throw new IdentityFailure(IdentityFailure.Reason.BAD_CREDENTIALS); }
        return flows.exchange(transaction, OpaqueTokens.fingerprint(code), challenge(verifier), mobileIdentity, clock.instant());
    }
    public void unlink(LoginIdentity identity, GoogleFlows.Channel channel) { enabled(); flows.unlink(identity, channel, clock.instant()); }
    public static String challenge(String verifier) {
        try { return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("Required PKCE digest unavailable."); }
    }
    private void enabled() { if (!policy.enabled()) { throw new IdentityFailure(IdentityFailure.Reason.FORBIDDEN); } }
}
