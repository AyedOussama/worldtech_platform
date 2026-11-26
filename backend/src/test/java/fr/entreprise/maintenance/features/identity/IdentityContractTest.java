package fr.entreprise.maintenance.features.identity;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import fr.entreprise.maintenance.bootstrap.health.HealthController;
import fr.entreprise.maintenance.features.identity.api.GoogleIdentityController;
import fr.entreprise.maintenance.features.identity.api.MobileIdentityController;
import fr.entreprise.maintenance.features.identity.api.WebIdentityActionsController;
import fr.entreprise.maintenance.features.identity.api.WebIdentityController;
import fr.entreprise.maintenance.features.identity.api.WebMfaController;
import fr.entreprise.maintenance.features.clients.api.ClientOnboardingController;
import fr.entreprise.maintenance.features.clients.api.ClientAccountController;
import fr.entreprise.maintenance.features.assets.api.AssetController;
import fr.entreprise.maintenance.features.contracts.api.ContractController;
import fr.entreprise.maintenance.features.tickets.api.TicketController;
import fr.entreprise.maintenance.features.missions.api.MissionController;
import fr.entreprise.maintenance.features.missions.api.MissionDiscoveryController;
import fr.entreprise.maintenance.features.technicians.api.TechnicianOnboardingController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityContractTest {
    @Test void staticOpenApiDocumentsExactlyTheDeliveredRoutesAndExplicitChannelSecurity() throws Exception {
        var document = JsonMapper.builder().build().readTree(Path.of("openapi/maintenance-api.json").toFile());
        Set<String> delivered = new HashSet<>();
        for (Class<?> controller : java.util.List.of(HealthController.class, WebIdentityController.class, WebIdentityActionsController.class,
                WebMfaController.class, MobileIdentityController.class, GoogleIdentityController.class,ClientOnboardingController.class,TechnicianOnboardingController.class,ClientAccountController.class,AssetController.class,ContractController.class,TicketController.class,
                MissionController.class,MissionDiscoveryController.class)) {
            for (var method : controller.getDeclaredMethods()) {
                var get = method.getAnnotation(GetMapping.class); var post = method.getAnnotation(PostMapping.class);
                var put=method.getAnnotation(PutMapping.class); var patch=method.getAnnotation(PatchMapping.class);
                if(put!=null) { for(String path:put.value()) { delivered.add("put "+path); } for(String path:put.path()) { delivered.add("put "+path); } }
                if(patch!=null) { for(String path:patch.value()) { delivered.add("patch "+path); } for(String path:patch.path()) { delivered.add("patch "+path); } }
                if (get != null) {
                    for (String path : get.value()) { delivered.add("get " + path); }
                    for (String path : get.path()) { delivered.add("get " + path); }
                }
                if (post != null) {
                    for (String path : post.value()) { delivered.add("post " + path); }
                    for (String path : post.path()) { delivered.add("post " + path); }
                }
            }
        }
        delivered.addAll(Set.of("post /api/web/v1/auth/login", "post /api/web/v1/auth/logout",
                "get /oauth2/authorization/google", "get /login/oauth2/code/google"));
        Set<String> documented = new HashSet<>();
        for (String path : document.get("paths").propertyNames()) {
            var operations = document.get("paths").get(path);
            for (String method : operations.propertyNames()) {
                documented.add(method + " " + path);
                var operation = operations.get(method);
                assertThat(operation.has("security")).as("Explicit security on %s %s", method, path).isTrue();
                if (path.startsWith("/api/web/") && Set.of("post","put","patch","delete").contains(method)) {
                    assertThat(operation.get("security").get(0).has("Csrf")).isTrue();
                }
            }
        }
        assertThat(documented).containsExactlyInAnyOrderElementsOf(delivered);
        assertThat(document.get("components").get("schemas").get("MeView").get("properties").propertyNames())
                .containsExactlyInAnyOrder("userId", "status", "roles", "emailVerified");
    }
}
