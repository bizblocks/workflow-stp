package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

/**
 * @author adiatullin
 */
public enum ComparingType implements EnumClass<Integer> {
    EQUALS(1, "=="),
    MORE(2, ">"),
    LESS(3, "<");

    private final Integer id;
    private final String operator;

    ComparingType(Integer id, String operator) {
        this.id = id;
        this.operator = operator;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public String getOperator() {
        return operator;
    }

    public static ComparingType fromId(Integer id) {
        if (id != null) {
            for (ComparingType i : values()) {
                if (i.getId().equals(id)) {
                    return i;
                }
            }
        }
        return null;
    }
}
