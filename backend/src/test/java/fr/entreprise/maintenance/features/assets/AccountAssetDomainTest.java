package fr.entreprise.maintenance.features.assets;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import fr.entreprise.maintenance.features.assets.domain.AssetFailure;
import fr.entreprise.maintenance.features.assets.domain.SiteProfile;
import fr.entreprise.maintenance.features.assets.domain.EquipmentProfile;
import fr.entreprise.maintenance.features.assets.domain.SiteAccess;
import fr.entreprise.maintenance.features.clients.domain.ClientAccount;
import fr.entreprise.maintenance.features.clients.domain.AccountFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountAssetDomainTest {
    @ParameterizedTest @ValueSource(strings={"Unknown/Paris","+02:00","GMT+01:00","Europe/Paris\n"})
    void aSiteRequiresAnExplicitIanaZoneRatherThanAnInventedOffset(String zone) {
        assertThatThrownBy(()->new SiteProfile("Site","Adresse",zone,List.of(),null).complete()).isInstanceOf(AssetFailure.class);
    }
    @Test void incompleteSitesAndEquipmentCannotBeStoredAndContactsArePrivateValidatedData() {
        assertThatThrownBy(()->new SiteProfile("Site",null,"Europe/Paris",List.of(),null).complete()).isInstanceOf(AssetFailure.class);
        assertThatThrownBy(()->new EquipmentProfile(null,"PC",null,null,null,null,null,null,Map.of()).complete()).isInstanceOf(AssetFailure.class);
        assertThatThrownBy(()->new SiteProfile.Contact("Contact",null,"not an email")).isInstanceOf(AssetFailure.class);
    }
    @ParameterizedTest @ValueSource(strings={"bad key","../../path","1key","a.b","owner_id\n"})
    void attributesHaveBoundedPlainKeysAndNeverContainArbitraryStructures(String key) {
        assertThatThrownBy(()->new EquipmentProfile("PC","POSTE",null,null,null,null,null,null,Map.of(key,"value"))).isInstanceOf(AssetFailure.class);
    }
    @Test void patchPreservesOmittedFieldsAndReplacementCanClearOptionalData() {
        var current=new SiteProfile("Site","Adresse","Europe/Paris",List.of(new SiteProfile.Contact("Contact",null,null)),"Consigne privée").complete();
        var patched=new SiteProfile("Nouveau",null,null,null,null).merge(current);
        assertThat(patched.address()).isEqualTo(current.address()); assertThat(patched.contacts()).hasSize(1);
        assertThat(new SiteProfile("Nouveau","Adresse","Europe/Paris",null,null).complete().contacts()).isEmpty();
    }
    @Test void allSitesIsAnExplicitChoiceAndCannotMixWithAHiddenPartialList() {
        UUID site=UUID.randomUUID();
        assertThatThrownBy(()->new SiteAccess(UUID.randomUUID(),true,List.of(site),0)).isInstanceOf(AssetFailure.class);
        assertThatThrownBy(()->new SiteAccess(UUID.randomUUID(),false,List.of(site,site),0)).isInstanceOf(AssetFailure.class);
    }
    @Test void individualMembershipCannotBeBroadenedAndLongValidNamesAreNotTruncated() {
        UUID user=UUID.randomUUID(); var account=new ClientAccount(UUID.randomUUID(),ClientAccount.Type.INDIVIDUAL,"Nom",user,0,Instant.EPOCH);
        account.allowMember(user);
        assertThatThrownBy(()->account.allowMember(UUID.randomUUID())).isInstanceOf(AccountFailure.class);
        assertThat(ClientAccount.name("a".repeat(100)+" "+"b".repeat(100))).hasSize(201);
    }
}
