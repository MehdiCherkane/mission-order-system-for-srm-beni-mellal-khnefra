package com.ordreDeMission.ordreDeMissison.service;

import com.ordreDeMission.ordreDeMissison.model.Employee;
import com.ordreDeMission.ordreDeMissison.model.Mission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

/**
 * Builds notifications while the mission data is available, then delivers them
 * only after the database transaction commits. Mail failures are deliberately
 * isolated from the workflow.
 */
@Service
public class MissionNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MissionNotificationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String portalBaseUrl;

    public MissionNotificationService(ApplicationEventPublisher eventPublisher,
                                      ObjectProvider<JavaMailSender> mailSenderProvider,
                                      @Value("${app.portal.base-url:http://localhost:8080}") String portalBaseUrl) {
        this.eventPublisher = eventPublisher;
        this.mailSenderProvider = mailSenderProvider;
        this.portalBaseUrl = portalBaseUrl;
    }

    public void missionSubmitted(Mission mission, Employee chef) {
        publish(mission, chef, "Nouvelle demande", "Vous avez une nouvelle demande à examiner.",
                "/chef/approval/" + mission.getId(), null);
    }

    public void chefApproved(Mission mission, Employee directeur) {
        publish(mission, mission.getRequester(), "En attente du directeur",
                "Votre demande a été approuvée par le chef hiérarchique et transmise au directeur provincial.",
                "/employee/mission/" + mission.getId(), null);
        publish(mission, directeur, "À valider", "Une demande approuvée par le chef attend votre décision.",
                "/directeur/approval/" + mission.getId(), null);
    }

    public void chefRejected(Mission mission, String reason) {
        publish(mission, mission.getRequester(), "Rejetée par le chef",
                "Votre demande a été rejetée par le chef hiérarchique.",
                "/employee/mission/" + mission.getId(), reason);
    }

    public void directeurApproved(Mission mission) {
        publish(mission, mission.getRequester(), "Approuvée",
                "Votre ordre de mission est validé et peut être imprimé depuis le portail.",
                "/employee/mission/" + mission.getId(), null);
    }

    public void directeurRejected(Mission mission, String reason) {
        publish(mission, mission.getRequester(), "Rejetée par le directeur",
                "Votre demande a été rejetée par le directeur provincial.",
                "/employee/mission/" + mission.getId(), reason);
    }

    private void publish(Mission mission, Employee recipient, String status, String message,
                         String path, String reason) {
        if (recipient == null) {
            log.warn("Notification ignorée pour la mission {} : destinataire introuvable", mission.getId());
            return;
        }
        try {
            eventPublisher.publishEvent(new MissionNotification(
                    recipient.getEmail(), fullName(recipient), mission.getObjet(), mission.getDestination(),
                    fullName(mission.getRequester()), mission.getDateDepart().format(DATE_FORMAT),
                    mission.getDateRetour().format(DATE_FORMAT), status, message, normalizeReason(reason),
                    portalUrl(path), mission.getId().toString()));
        } catch (RuntimeException ex) {
            log.warn("Impossible de mettre en file la notification de mission {} : {}", mission.getId(), ex.getMessage());
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(MissionNotification notification) {
        if (!isUsableEmail(notification.recipientEmail())) {
            log.warn("Notification ignorée pour la mission {} : e-mail destinataire absent ou invalide",
                    notification.missionId());
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Notification ignorée pour la mission {} : SMTP non configuré", notification.missionId());
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(notification.recipientEmail());
            mail.setSubject("Ordre de mission — " + notification.object() + " — " + notification.status());
            mail.setText(body(notification));
            mailSender.send(mail);
        } catch (RuntimeException ex) {
            log.warn("Échec d'envoi de la notification de mission {} à {} : {}",
                    notification.missionId(), notification.recipientEmail(), ex.getMessage());
        }
    }

    private String body(MissionNotification notification) {
        String reason = notification.reason() == null ? "" : "\nMotif : " + notification.reason() + "\n";
        return "SRM Missions\n\n"
                + "Bonjour " + notification.recipientName() + ",\n\n"
                + notification.message() + "\n\n"
                + "Demandeur : " + notification.requesterName() + "\n"
                + "Objet : " + notification.object() + "\n"
                + "Destination : " + notification.destination() + "\n"
                + "Départ : " + notification.departureDate() + "\n"
                + "Retour : " + notification.returnDate() + "\n"
                + reason + "\nAccéder à la demande : " + notification.url() + "\n\n"
                + "— SRM Missions";
    }

    private String portalUrl(String path) {
        String base = portalBaseUrl == null ? "http://localhost:8080" : portalBaseUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + path;
    }

    private static String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    private static boolean isUsableEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private static String fullName(Employee employee) {
        return employee.getPrenom() + " " + employee.getNom();
    }

    public record MissionNotification(String recipientEmail, String recipientName, String object,
                                      String destination, String requesterName, String departureDate,
                                      String returnDate, String status, String message, String reason,
                                      String url, String missionId) {
    }
}
