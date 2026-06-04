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

    public PartnerMailEvent(Object source, Long historyId, String partenaireName, String emailTo, String[] ccEmails, byte[] excelAttachment, String messageTemplate) {
        super(source);
        this.historyId = historyId;
        this.partenaireName = partenaireName;
        this.emailTo = emailTo;
        this.ccEmails = ccEmails;
        this.excelAttachment = excelAttachment;
        this.messageTemplate = messageTemplate;
    }
}