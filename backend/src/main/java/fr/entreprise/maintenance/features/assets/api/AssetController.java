package fr.entreprise.maintenance.features.assets.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import fr.entreprise.maintenance.features.assets.application.AssetService;
import fr.entreprise.maintenance.features.assets.domain.AssetFailure;
import fr.entreprise.maintenance.features.assets.domain.Equipment;
import fr.entreprise.maintenance.features.assets.domain.EquipmentProfile;
import fr.entreprise.maintenance.features.assets.domain.Site;
import fr.entreprise.maintenance.features.assets.domain.SiteAccess;
import fr.entreprise.maintenance.features.assets.domain.SiteProfile;
import fr.entreprise.maintenance.features.identity.publicapi.IdentitySession;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AssetController {
    private final AssetService assets;
    public AssetController(AssetService assets) { this.assets=assets; }
    @GetMapping({"/api/web/v1/admin/client-accounts/{accountId}/sites","/api/web/v1/client/accounts/{accountId}/sites"})
    public ResponseEntity<SitesPage> sites(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        var p=assets.sites(actor,accountId,admin(request),page,size); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new SitesPage(p.items().stream().map(AssetController::site).toList(),p.total(),p.page(),p.size()));
    }
    @PostMapping("/api/web/v1/admin/client-accounts/{accountId}/sites")
    public ResponseEntity<SiteView> createSite(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@Valid @RequestBody SiteBody body) {
        var s=assets.createSite(actor,accountId,body.profile()); return ResponseEntity.created(URI.create("/api/web/v1/admin/client-accounts/"+accountId+"/sites/"+s.siteId())).cacheControl(CacheControl.noStore()).eTag(tag(s.version())).body(site(s));
    }
    @GetMapping({"/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}","/api/web/v1/client/accounts/{accountId}/sites/{siteId}"})
    public ResponseEntity<SiteView> site(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,HttpServletRequest request) { return response(assets.site(actor,accountId,siteId,admin(request))); }
    @PutMapping("/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}")
    public ResponseEntity<SiteView> replaceSite(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@Valid @RequestBody SiteBody body,@RequestHeader("If-Match") String expected) { return response(assets.updateSite(actor,accountId,siteId,body.profile(),version(expected),false)); }
    @PatchMapping("/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}")
    public ResponseEntity<SiteView> patchSite(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@Valid @RequestBody SiteBody body,@RequestHeader("If-Match") String expected) { return response(assets.updateSite(actor,accountId,siteId,body.profile(),version(expected),true)); }
    @PostMapping("/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/archive")
    public ResponseEntity<SiteView> archiveSite(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@RequestHeader("If-Match") String expected) { return response(assets.archiveSite(actor,accountId,siteId,version(expected))); }
    @GetMapping({"/api/web/v1/admin/client-accounts/{accountId}/equipment","/api/web/v1/client/accounts/{accountId}/equipment"})
    public ResponseEntity<EquipmentPage> equipment(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@RequestParam(required=false) UUID siteId,@RequestParam(required=false) String category,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        var p=assets.equipment(actor,accountId,admin(request),siteId,category,page,size); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new EquipmentPage(p.items().stream().map(AssetController::equipment).toList(),p.total(),p.page(),p.size()));
    }
    @PostMapping("/api/web/v1/admin/client-accounts/{accountId}/sites/{siteId}/equipment")
    public ResponseEntity<EquipmentView> createEquipment(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID siteId,@Valid @RequestBody EquipmentBody body) {
        var e=assets.createEquipment(actor,accountId,siteId,body.profile()); return ResponseEntity.created(URI.create("/api/web/v1/admin/client-accounts/"+accountId+"/equipment/"+e.equipmentId())).cacheControl(CacheControl.noStore()).eTag(tag(e.version())).body(equipment(e));
    }
    @GetMapping({"/api/web/v1/admin/client-accounts/{accountId}/equipment/{equipmentId}","/api/web/v1/client/accounts/{accountId}/equipment/{equipmentId}"})
    public ResponseEntity<EquipmentView> equipmentDetail(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID equipmentId,HttpServletRequest request) { return response(assets.equipmentDetail(actor,accountId,equipmentId,admin(request))); }
    @PutMapping("/api/web/v1/admin/client-accounts/{accountId}/equipment/{equipmentId}")
    public ResponseEntity<EquipmentView> replaceEquipment(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID equipmentId,@Valid @RequestBody EquipmentBody body,@RequestHeader("If-Match") String expected) { return response(assets.updateEquipment(actor,accountId,equipmentId,body.profile(),version(expected),false)); }
    @PatchMapping("/api/web/v1/admin/client-accounts/{accountId}/equipment/{equipmentId}")
    public ResponseEntity<EquipmentView> patchEquipment(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID equipmentId,@Valid @RequestBody EquipmentBody body,@RequestHeader("If-Match") String expected) { return response(assets.updateEquipment(actor,accountId,equipmentId,body.profile(),version(expected),true)); }
    @PostMapping("/api/web/v1/admin/client-accounts/{accountId}/equipment/{equipmentId}/archive")
    public ResponseEntity<EquipmentView> archiveEquipment(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID equipmentId,@RequestHeader("If-Match") String expected) { return response(assets.archiveEquipment(actor,accountId,equipmentId,version(expected))); }
    @GetMapping({"/api/web/v1/admin/client-accounts/{accountId}/summary","/api/web/v1/client/accounts/{accountId}/summary"})
    public ResponseEntity<SummaryView> summary(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,HttpServletRequest request) { var s=assets.summary(actor,accountId,admin(request)); return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new SummaryView(s.sites(),s.equipment())); }
    @GetMapping("/api/web/v1/admin/client-accounts/{accountId}/members/{userId}/site-access")
    public ResponseEntity<AccessView> access(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID userId) { return access(assets.access(actor,accountId,userId)); }
    @PutMapping("/api/web/v1/admin/client-accounts/{accountId}/members/{userId}/site-access")
    public ResponseEntity<AccessView> replaceAccess(@AuthenticationPrincipal IdentitySession actor,@PathVariable UUID accountId,@PathVariable UUID userId,@Valid @RequestBody AccessBody body,@RequestHeader("If-Match") String expected) { return access(assets.replaceAccess(actor,accountId,userId,body.allSites(),body.siteIds(),version(expected))); }
    private static ResponseEntity<AccessView> access(SiteAccess p) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(p.version())).body(new AccessView(p.memberId(),p.allSites(),p.siteIds(),p.version())); }
    private static ResponseEntity<SiteView> response(Site s) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(s.version())).body(site(s)); }
    private static ResponseEntity<EquipmentView> response(Equipment e) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(tag(e.version())).body(equipment(e)); }
    private static SiteView site(Site s) { var p=s.profile(); return new SiteView(s.siteId(),new SiteBody(p.name(),p.address(),p.timeZone(),p.contacts().stream().map(c->new ContactBody(c.name(),c.phone(),c.email())).toList(),p.accessInstructions()),s.archived(),s.version()); }
    private static EquipmentView equipment(Equipment e) { var p=e.profile(); return new EquipmentView(e.equipmentId(),e.siteId(),new EquipmentBody(p.label(),p.category(),p.internalReference(),p.brand(),p.model(),p.serialNumber(),p.location(),p.conditionDescription(),new LinkedHashMap<>(p.attributes())),e.archived(),e.version()); }
    private static boolean admin(HttpServletRequest request) { return request.getRequestURI().startsWith("/api/web/v1/admin/"); }
    private static String tag(long v) { return "\""+v+"\""; }
    private static long version(String value) { if(!value.matches("\"[0-9]{1,18}\"")) { throw new AssetFailure(AssetFailure.Code.INVALID_ASSET_INPUT); } return Long.parseLong(value.substring(1,value.length()-1)); }
    public record ContactBody(@NotNull @Size(min=1,max=100) String name,@Size(max=40) String phone,@Size(max=254) String email) { SiteProfile.Contact contact() { return new SiteProfile.Contact(name,phone,email); } @Override public String toString() { return "ContactBody[redacted]"; } }
    public record SiteBody(@Size(max=120) String name,@Size(max=500) String address,@Size(max=100) String timeZone,@Size(max=10) List<@NotNull @Valid ContactBody> contacts,@Size(max=2000) String accessInstructions) {
        SiteProfile profile() { return new SiteProfile(name,address,timeZone,contacts==null?null:contacts.stream().map(ContactBody::contact).toList(),accessInstructions); }
        @Override public String toString() { return "SiteBody[redacted]"; }
    }
    public record EquipmentBody(@Size(max=120) String label,@Size(max=80) String category,@Size(max=100) String internalReference,@Size(max=100) String brand,@Size(max=100) String model,@Size(max=100) String serialNumber,@Size(max=200) String location,@Size(max=200) String conditionDescription,@Size(max=20) Map<String,Object> attributes) {
        EquipmentProfile profile() {
            Map<String,String> converted=null;
            if(attributes!=null) { converted=new LinkedHashMap<>(); for(var e:attributes.entrySet()) { if(!(e.getValue() instanceof String value)) { throw new AssetFailure(AssetFailure.Code.INVALID_ASSET_INPUT); } converted.put(e.getKey(),value); } }
            return new EquipmentProfile(label,category,internalReference,brand,model,serialNumber,location,conditionDescription,converted);
        }
        @Override public String toString() { return "EquipmentBody[redacted]"; }
    }
    public record AccessBody(@NotNull Boolean allSites,@NotNull @Size(max=100) List<@NotNull UUID> siteIds) { }
    public record SiteView(UUID siteId,SiteBody profile,boolean archived,long version) { }
    public record EquipmentView(UUID equipmentId,UUID siteId,EquipmentBody profile,boolean archived,long version) { }
    public record AccessView(UUID memberId,boolean allSites,List<UUID> siteIds,long version) { }
    public record SitesPage(List<SiteView> items,long total,int page,int size) { }
    public record EquipmentPage(List<EquipmentView> items,long total,int page,int size) { }
    public record SummaryView(long sites,long equipment) { }
}
