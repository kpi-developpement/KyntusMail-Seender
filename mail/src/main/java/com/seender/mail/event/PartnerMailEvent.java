package com.seender.mail.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PartnerMailEvent extends ApplicationEvent {

    private final Long historyId; // Zdnaha bach n-trackew l'mail f DB
    private final String partenaireName;
    private final String emailTo;
    private final String[] ccEmails;
    private final byte[] excelAttachment;
    private final String messageTemplate;
    private final String subject;
    private final String attachmentName;

    // Flux "Anomalies" (comportement historique)
    public PartnerMailEvent(Object source, Long historyId, String partenaireName, String emailTo, String[] ccEmails, byte[] excelAttachment, String messageTemplate) {
        this(source, historyId, partenaireName, emailTo, ccEmails, excelAttachment, messageTemplate,
                "TR: Rapport des anomalies - " + partenaireName,
                "Anomalies_" + partenaireName + ".xlsx");
    }

    // Flux générique (ex: envoi des comptes / identifiants)
    public PartnerMailEvent(Object source, Long historyId, String partenaireName, String emailTo, String[] ccEmails, byte[] excelAttachment, String messageTemplate, String subject, String attachmentName) {
        super(source);
        this.historyId = historyId;
        this.partenaireName = partenaireName;
        this.emailTo = emailTo;
        this.ccEmails = ccEmails;
        this.excelAttachment = excelAttachment;
        this.messageTemplate = messageTemplate;
        this.subject = subject;
        this.attachmentName = attachmentName;
    }
}
