package fr.entreprise.maintenance.features.assets.publicapi;

import java.util.UUID;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;

/** Authority for one currently accessible, active site, valid only in the caller's transaction. */
public interface AssetAccess {
    record SiteContext(UUID accountId, UUID siteId, UUID actorUserId, String timeZone, boolean administrator) { }
    SiteContext authorizeSite(IdentitySession actor, UUID accountId, UUID siteId, boolean administratorOnly);
    void requireEquipment(SiteContext site, UUID equipmentId);
}
