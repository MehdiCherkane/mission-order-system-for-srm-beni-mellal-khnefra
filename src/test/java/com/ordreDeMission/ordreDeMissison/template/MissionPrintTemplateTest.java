package com.ordreDeMission.ordreDeMissison.template;

import com.ordreDeMission.ordreDeMissison.model.ApprovalStep;
import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.model.Mission;
import com.ordreDeMission.ordreDeMissison.model.MissionParticipant;
import com.ordreDeMission.ordreDeMissison.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebEngineContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the printable Ordre de Mission with stressful dynamic data
 * (very long motif, long mixed French/Arabic destination) and checks the
 * document stays complete and well-formed. Also writes the rendered HTML
 * to target/print-sample.html for visual/PDF inspection.
 */
class MissionPrintTemplateTest {

    private SpringTemplateEngine engine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
    }

    private static Mission sampleMission() {
        Employee requester = new Employee("EMP001", "Benali", "Ahmed El Idrissi El Fassi",
                "Technicien", "Service Eau", "Direction Technique",
                "EMPLOYEE", "ahmed.benali@srm.ma", "hash");
        Employee chef = new Employee("CHEF001", "Idrissi", "Karim",
                "Chef de Service", "Service Technique", "Direction Provinciale",
                "CHEF_HIERARCHIQUE", "karim.idrissi@srm.ma", "hash");
        Employee directeur = new Employee("DIR001", "El Amrani", "Nadia",
                "Directeur Provincial", "Direction Provinciale", "Direction Provinciale",
                "DIRECTEUR", "nadia.elamrani@srm.ma", "hash");
        Employee other = new Employee("EMP003", "Amrani", "Youssef",
                "Technicien", "Service Eau", "Direction Technique",
                "EMPLOYEE", "youssef.amrani@srm.ma", "hash");

        Mission mission = new Mission();
        mission.setId(UUID.fromString("8f6ceb4c-1ce6-442e-a4cb-3c4d4b545b3b"));
        mission.setRequester(requester);
        // ~500 chars: worst case allowed by the creation form
        String motif = "Intervention technique suite à une fuite sur le réseau de distribution d'eau potable. "
                + "Remplacement du tronçon endommagé, purge complète, désinfection, remise en service avec contrôle "
                + "de pression et analyse de qualité de l'eau avant réouverture aux usagers du quartier concerné. "
                + "Coordination avec les autorités locales et information des riverains pendant toute la durée.";
        mission.setObjet(motif);
        mission.setDestination("Khénifra, El Kbab, quartier Al Fath, rue Jasmin prolongée vers le haut du douar Aït Said");
        mission.setDateDepart(LocalDateTime.of(2026, 9, 26, 9, 0));
        mission.setDateRetour(LocalDateTime.of(2026, 9, 26, 13, 0));
        mission.setMoyenTransport("vehicule_de_service");
        mission.setVehicle(new Vehicle("5678-B-1234", "Renault Kangoo", "Service Eau"));
        mission.setCombine(true);
        mission.setNumero(3);
        mission.setAnnee(2026);
        mission.getParticipants().add(new MissionParticipant(mission, other));

        ApprovalStep step1 = new ApprovalStep(mission, chef, 1);
        step1.setStatut("approuve");
        step1.setDateAction(LocalDateTime.of(2026, 9, 25, 10, 15));
        ApprovalStep step2 = new ApprovalStep(mission, directeur, 2);
        step2.setStatut("approuve");
        step2.setDateAction(LocalDateTime.of(2026, 9, 25, 16, 40));
        mission.getApprovalSteps().add(step1);
        mission.getApprovalSteps().add(step2);
        return mission;
    }

    private String render(Mission mission) {
        // Real servlet web exchange so @{...} links resolve exactly like production
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication application =
                JakartaServletWebApplication.buildApplication(servletContext);
        IWebExchange exchange = application.buildExchange(request, response);
        WebEngineContext context = new WebEngineContext(
                engine.getConfiguration(), null, null, exchange,
                Locale.FRANCE, Map.of("mission", mission));
        return engine.process("employee/mission-print", context);
    }

    @Test
    void printRendersCompleteDocumentWithLongData() throws Exception {
        Mission mission = sampleMission();
        String html = render(mission);

        Files.write(Paths.get("target/print-sample.html"), html.getBytes(StandardCharsets.UTF_8));

        // Title + sequential reference (year comes from the data, not hardcoded)
        assertTrue(html.contains("ORDRE DE MISSION N°"));
        assertTrue(html.contains("0003/2026"));

        // Arabic company identity survives rendering as real Unicode text
        assertTrue(html.contains("الشركة الجهوية متعددة الخدمات"));
        assertTrue(html.contains("المرجع"));
        // Tifinagh line survives rendering (codepoints U+2D30..U+2D7F)
        assertTrue(html.codePoints().anyMatch(cp -> cp >= 0x2D30 && cp <= 0x2D7F));

        // Full long motif present (nothing truncated or lost; apostrophes are HTML-escaped by Thymeleaf)
        String escapedMotif = mission.getObjet().replace("'", "&#39;");
        assertTrue(html.contains(escapedMotif));
        assertTrue(html.contains(mission.getDestination()));

        // Both workflow visas with names and dates
        assertTrue(html.contains("Visa du Chef Hiérarchique"));
        assertTrue(html.contains("Visa du Directeur Provincial"));
        assertTrue(html.contains("Karim Idrissi"));
        assertTrue(html.contains("Nadia El Amrani"));
        assertTrue(html.contains("25/09/2026"));

        // Vehicle + participant rendered
        assertTrue(html.contains("5678-B-1234 - Renault Kangoo"));
        assertTrue(html.contains("Youssef Amrani"));

        // Print infrastructure: A4 page, print stylesheet, controls hidden on paper
        assertTrue(html.contains("@page"));
        assertTrue(html.contains("@media print"));
        assertTrue(html.contains(".no-print"));

        // Language contexts separated
        assertTrue(html.contains("lang=\"ar\""));
        assertTrue(html.contains("dir=\"rtl\""));

        // No leftover template expressions
        assertFalse(html.contains("${"));
    }

    @Test
    void printHandlesPersonalVehicleWithoutParticipants() {
        Mission mission = sampleMission();
        mission.setVehicle(null);
        mission.setMoyenTransport("vehicule_personnel");
        mission.setCombine(false);
        mission.getParticipants().clear();

        String html = render(mission);

        assertTrue(html.contains("avec son véhicule personnel"));
        assertFalse(html.contains("Participants accompagnateurs"));
    }
}
