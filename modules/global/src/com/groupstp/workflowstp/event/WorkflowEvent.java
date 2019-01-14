package com.groupstp.workflowstp.event;

import com.haulmont.cuba.core.global.UuidProvider;
import org.springframework.context.ApplicationEvent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * This event using for notifying listeners about workflow process changes
 * Current and previous may be the same if workflow was restarted
 *
 * @author adiatullin
 */
public class WorkflowEvent extends ApplicationEvent {
    private static final long serialVersionUID = 8677302494540653365L;

    private final Class entityClass;
    private final String entityId;
    private final Class entityIdClass;
    private final String currentStage;
    private final String previousStage;

    public WorkflowEvent(Class entityClass, String entityId, Class entityIdClass, String currentStage, @Nullable String previousStage) {
        super("workflow");

        this.entityClass = entityClass;
        this.entityId = entityId;
        this.entityIdClass = entityIdClass;
        this.currentStage = currentStage;
        this.previousStage = previousStage;
    }

    public Class getEntityClass() {
        return entityClass;
    }

    @SuppressWarnings("unchecked")
    public <T> T getEntityId() {
        if (UUID.class.isAssignableFrom(entityIdClass)) {
            return (T) UuidProvider.fromString(entityId);
        } else if (Integer.class.isAssignableFrom(entityIdClass)) {
            return (T) Integer.valueOf(entityId);
        } else if (Long.class.isAssignableFrom(entityIdClass)) {
            return (T) Long.valueOf(entityId);
        } else if (String.class.isAssignableFrom(entityIdClass)) {
            return (T) entityId;
        } else {
            throw new UnsupportedOperationException(String.format("Unknown entity id '%s'", entityId));
        }
    }

    public String getCurrentStage() {
        return currentStage;
    }

    @Nullable
    public String getPreviousStage() {
        return previousStage;
    }
}
