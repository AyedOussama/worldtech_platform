package fr.entreprise.maintenance.features.assets.application;

import java.util.UUID;
import fr.entreprise.maintenance.features.assets.domain.AssetFailure;
import fr.entreprise.maintenance.features.assets.domain.AssetStore;
import fr.entreprise.maintenance.features.assets.publicapi.AssetAccess;
import fr.entreprise.maintenance.features.clients.publicapi.ClientAccountAccess;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation=Propagation.MANDATORY)
public class AssetAccessService implements AssetAccess {
    private final ClientAccountAccess clients;
    private final AssetStore assets;
    public AssetAccessService(ClientAccountAccess clients, AssetStore assets) { this.clients=clients; this.assets=assets; }
    @Override public SiteContext authorizeSite(IdentitySession actor, UUID accountId, UUID siteId, boolean administratorOnly) {
        var scope=clients.authorize(actor,accountId,administratorOnly);
        var site=assets.site(accountId,scope.memberId(),scope.administrator(),siteId);
        return new SiteContext(accountId,siteId,scope.actorUserId(),site.profile().timeZone(),scope.administrator());
    }
    @Override public void requireEquipment(SiteContext site, UUID equipmentId) {
        if(!assets.equipmentBelongsToActiveSite(site.accountId(),site.siteId(),equipmentId)) { throw new AssetFailure(AssetFailure.Code.ASSET_NOT_FOUND); }
    }
}
