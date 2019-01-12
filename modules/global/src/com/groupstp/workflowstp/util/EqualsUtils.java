package com.groupstp.workflowstp.util;

import java.util.Objects;

/*
 * @author adiatullin
 */
public final class EqualsUtils {

    private EqualsUtils() {
    }

    /**
     * Check is provided object equals any other objects
     *
     * @param o     comparing object
     * @param other other objects
     * @return equal any from other or no
     */
    public static boolean equalAny(Object o, Object... other) {
        if (other != null && other.length > 0) {
            for (Object i : other) {
                if (Objects.equals(o, i)) {
                    return true;
                }
            }
        }
        return false;
    }
}
