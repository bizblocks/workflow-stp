package com.groupstp.workflowstp.web.util;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UuidProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * Workflow instance helper class
 *
 * @author adiatullin
 */
public final class WorkflowInstanceHelper {
    private WorkflowInstanceHelper() {
    }

    /**
     * Parse entity ID to valid java object
     *
     * @param entityName entity name
     * @param entityId   entity ID in string format
     * @return ID in valid java object
     */
    public static Object parseEntityId(String entityName, String entityId) {
        if (!StringUtils.isEmpty(entityId) && !StringUtils.isEmpty(entityId)) {
            Metadata metadata = AppBeans.get(Metadata.NAME);
            MetaClass metaClass = metadata.getClassNN(entityName);
            MetaProperty idProperty = metaClass.getPropertyNN("id");
            Class idClass = idProperty.getJavaType();
            if (UUID.class.isAssignableFrom(idClass)) {
                return UuidProvider.fromString(entityId);
            } else if (Integer.class.isAssignableFrom(idClass)) {
                return Integer.valueOf(entityId);
            } else if (Long.class.isAssignableFrom(idClass)) {
                return Long.valueOf(entityId);
            } else if (String.class.isAssignableFrom(idClass)) {
                return entityId;
            } else {
                throw new UnsupportedOperationException(String.format("Unknown entity '%s' id type '%s'", entityName, entityId));
            }
        }
        return null;
    }
}
