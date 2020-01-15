package com.groupstp.workflowstp.annotation;

import com.haulmont.cuba.core.entity.annotation.MetaAnnotation;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Additional meta data of workflow behaviour for each workflow entity
 *
 * @author adiatullin
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@MetaAnnotation
public @interface WorkflowDetails {

    /**
     * @return expected extending browser screen id
     */
    String browseId() default StringUtils.EMPTY;

    /**
     * @return expected extending editor screen id
     */
    String editorId() default StringUtils.EMPTY;
}
