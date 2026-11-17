package fr.entreprise.maintenance.features.assets.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AssetStore {
    Site site(UUID account,UUID member,boolean admin,UUID site);
    List<Site> sites(UUID account,UUID member,boolean admin,int offset,int size);
    long siteCount(UUID account,UUID member,boolean admin);
    Site createSite(UUID account,SiteProfile profile,UUID actor,Instant now);
    Site updateSite(UUID account,Site current,SiteProfile profile,boolean archive,UUID actor,Instant now);
    Equipment equipment(UUID account,UUID member,boolean admin,UUID equipment);
    boolean equipmentBelongsToActiveSite(UUID account,UUID site,UUID equipment);
    List<Equipment> equipmentList(UUID account,UUID member,boolean admin,UUID site,String category,int offset,int size);
    long equipmentCount(UUID account,UUID member,boolean admin,UUID site,String category);
    Equipment createEquipment(UUID account,UUID site,EquipmentProfile profile,UUID actor,Instant now);
    Equipment updateEquipment(UUID account,Equipment current,EquipmentProfile profile,boolean archive,UUID actor,Instant now);
    SiteAccess access(UUID account,UUID member);
    SiteAccess replaceAccess(UUID account,SiteAccess current,boolean allSites,List<UUID> sites,UUID actor,Instant now);
}
