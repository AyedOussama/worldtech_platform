package fr.entreprise.maintenance.features.assets.domain;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public record SiteAccess(UUID memberId,boolean allSites,List<UUID> siteIds,long version) {
    public SiteAccess {
        if(siteIds==null || siteIds.size()>100 || siteIds.stream().anyMatch(id->id==null) || new HashSet<>(siteIds).size()!=siteIds.size() || (allSites && !siteIds.isEmpty())) { throw AssetText.invalid(); }
        siteIds=siteIds.stream().sorted().toList();
    }
    public void expect(long expected) { if(version!=expected) { throw new AssetFailure(AssetFailure.Code.ASSET_VERSION_STALE); } }
}
