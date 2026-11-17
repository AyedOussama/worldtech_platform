package fr.entreprise.maintenance.features.assets.domain;

import java.util.UUID;

public record Site(UUID siteId,SiteProfile profile,boolean archived,long version) {
    public void expect(long expected) { if(version!=expected) { throw new AssetFailure(AssetFailure.Code.ASSET_VERSION_STALE); } }
    @Override public String toString() { return "Site[redacted]"; }
}
