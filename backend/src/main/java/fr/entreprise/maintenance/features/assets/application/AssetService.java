package fr.entreprise.maintenance.features.assets.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import fr.entreprise.maintenance.features.assets.domain.AssetFailure;
import fr.entreprise.maintenance.features.assets.domain.AssetStore;
import fr.entreprise.maintenance.features.assets.domain.Equipment;
import fr.entreprise.maintenance.features.assets.domain.EquipmentProfile;
import fr.entreprise.maintenance.features.assets.domain.Site;
import fr.entreprise.maintenance.features.assets.domain.SiteAccess;
import fr.entreprise.maintenance.features.assets.domain.SiteProfile;
import fr.entreprise.maintenance.features.clients.publicapi.ClientAccountAccess;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssetService {
    public record SitePage(List<Site> items,long total,int page,int size) { }
    public record EquipmentPage(List<Equipment> items,long total,int page,int size) { }
    public record Summary(long sites,long equipment) { }
    private final AssetStore store;
    private final ClientAccountAccess clients;
    private final Clock clock;
    public AssetService(AssetStore store,ClientAccountAccess clients,Clock clock) { this.store=store; this.clients=clients; this.clock=clock; }
    public SitePage sites(IdentitySession actor,UUID account,boolean admin,int page,int size) {
        bounds(page,size); var s=clients.authorize(actor,account,admin);
        return new SitePage(store.sites(account,s.memberId(),s.administrator(),page*size,size),store.siteCount(account,s.memberId(),s.administrator()),page,size);
    }
    public Site site(IdentitySession actor,UUID account,UUID id,boolean admin) { var s=clients.authorize(actor,account,admin); return store.site(account,s.memberId(),s.administrator(),id); }
    public Site createSite(IdentitySession actor,UUID account,SiteProfile profile) { clients.authorize(actor,account,true); return store.createSite(account,profile.complete(),actor.userId(),clock.instant()); }
    public Site updateSite(IdentitySession actor,UUID account,UUID id,SiteProfile profile,long expected,boolean patch) {
        clients.authorize(actor,account,true); var current=store.site(account,null,true,id); current.expect(expected);
        return store.updateSite(account,current,patch?profile.merge(current.profile()):profile.complete(),false,actor.userId(),clock.instant());
    }
    public Site archiveSite(IdentitySession actor,UUID account,UUID id,long expected) {
        clients.authorize(actor,account,true); var current=store.site(account,null,true,id); current.expect(expected);
        return store.updateSite(account,current,current.profile(),true,actor.userId(),clock.instant());
    }
    public EquipmentPage equipment(IdentitySession actor,UUID account,boolean admin,UUID site,String category,int page,int size) {
        bounds(page,size); var s=clients.authorize(actor,account,admin);
        if(site!=null) { store.site(account,s.memberId(),s.administrator(),site); }
        if(category!=null && (category.isBlank() || category.length()>80 || category.chars().anyMatch(Character::isISOControl))) { throw invalid(); }
        return new EquipmentPage(store.equipmentList(account,s.memberId(),s.administrator(),site,category,page*size,size),store.equipmentCount(account,s.memberId(),s.administrator(),site,category),page,size);
    }
    public Equipment equipmentDetail(IdentitySession actor,UUID account,UUID id,boolean admin) { var s=clients.authorize(actor,account,admin); return store.equipment(account,s.memberId(),s.administrator(),id); }
    public Equipment createEquipment(IdentitySession actor,UUID account,UUID site,EquipmentProfile p) { clients.authorize(actor,account,true); store.site(account,null,true,site); return store.createEquipment(account,site,p.complete(),actor.userId(),clock.instant()); }
    public Equipment updateEquipment(IdentitySession actor,UUID account,UUID id,EquipmentProfile profile,long expected,boolean patch) {
        clients.authorize(actor,account,true); var c=store.equipment(account,null,true,id); c.expect(expected);
        return store.updateEquipment(account,c,patch?profile.merge(c.profile()):profile.complete(),false,actor.userId(),clock.instant());
    }
    public Equipment archiveEquipment(IdentitySession actor,UUID account,UUID id,long expected) {
        clients.authorize(actor,account,true); var c=store.equipment(account,null,true,id); c.expect(expected);
        return store.updateEquipment(account,c,c.profile(),true,actor.userId(),clock.instant());
    }
    public Summary summary(IdentitySession actor,UUID account,boolean admin) { var s=clients.authorize(actor,account,admin); return new Summary(store.siteCount(account,s.memberId(),s.administrator()),store.equipmentCount(account,s.memberId(),s.administrator(),null,null)); }
    public SiteAccess access(IdentitySession actor,UUID account,UUID user) { var s=clients.authorize(actor,account,true); return store.access(account,clients.currentMember(s,user)); }
    public SiteAccess replaceAccess(IdentitySession actor,UUID account,UUID user,boolean all,List<UUID> sites,long expected) {
        var s=clients.authorize(actor,account,true); UUID member=clients.currentMember(s,user);
        var current=store.access(account,member); current.expect(expected); var checked=new SiteAccess(member,all,sites,expected);
        for(UUID site:checked.siteIds()) { store.site(account,null,true,site); }
        return store.replaceAccess(account,current,all,checked.siteIds(),actor.userId(),clock.instant());
    }
    private static void bounds(int page,int size) { if(page<0 || page>10000 || size<1 || size>100) { throw invalid(); } }
    private static AssetFailure invalid() { return new AssetFailure(AssetFailure.Code.INVALID_ASSET_INPUT); }
}
