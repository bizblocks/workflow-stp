package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

/**
 * Workflow entities status enum
 *
 * @author adiatullin
 */
public enum WorkflowEntityStatus implements EnumClass<Integer> {
    IN_PROGRESS(1),
    DONE(2),
    FAILED(3);

    private final int id;

    WorkflowEntityStatus(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public static WorkflowEntityStatus fromId(Integer id) {
        if (id != null) {
            for (WorkflowEntityStatus i : values()) {
                if (i.getId().equals(id)) {
                    return i;
                }
            }
        }
        return null;
    }
}
