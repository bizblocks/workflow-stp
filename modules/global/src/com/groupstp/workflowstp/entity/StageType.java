package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

/**
 * Workflow stage type
 *
 * @author adiatullin
 */
public enum StageType implements EnumClass<Integer> {
    USERS_INTERACTION(1),
    ALGORITHM_EXECUTION(2),
    ARCHIVE(3);

    private final Integer id;

    StageType(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public static StageType fromId(Integer id) {
        if (id != null) {
            for (StageType i : values()) {
                if (id.equals(i.getId())) {
                    return i;
                }
            }
        }
        return null;
    }
}
