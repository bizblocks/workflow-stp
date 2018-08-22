package com.groupstp.workflowstp.core.bean;

import com.haulmont.cuba.core.global.Messages;

import javax.inject.Inject;
import java.util.Objects;

/**
 * Useful abstract bean for deep localization
 *
 * @author adiatullin
 */
public abstract class MessageableBean {
    private Messages messages;

    @Inject
    public void setMessages(Messages messages) {
        this.messages = messages;
    }

    /**
     * Get deep search localized message by key
     *
     * @param messageKey message key
     * @return localized message
     */
    protected String getMessage(String messageKey) {
        Class clazz = getClass();
        String message = messages.getMessage(clazz.getPackage().getName(), messageKey);
        while (Objects.equals(messageKey, message)) {//do while message not found
            clazz = clazz.getSuperclass();
            if (clazz == null) return messageKey;
            message = messages.getMessage(clazz.getPackage().getName(), messageKey);
        }
        return message;
    }
}
