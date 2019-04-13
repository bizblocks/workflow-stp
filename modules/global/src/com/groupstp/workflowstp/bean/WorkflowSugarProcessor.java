package com.groupstp.workflowstp.bean;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Special helper class which parse and prepare workflow scripts to support helpful stuff
 *
 * @author adiatullin
 */
@Component(WorkflowSugarProcessor.NAME)
public class WorkflowSugarProcessor {
    public static final String NAME = "wfstp_WorkflowSugarProcessor";

    private Pattern pattern = Pattern.compile("\\$\\{[^}]+\\}");

    /**
     * Make preparation of specified script
     *
     * @param script current processing script
     * @return processed script
     */
    public String prepareScript(String script) {
        if (!StringUtils.isEmpty(script)) {
            script = appendImports(script);
            script = substituteParameters(script);
        }
        return script;
    }

    protected String appendImports(String script) {
        return "import com.haulmont.cuba.core.global.*;\n" +
                "import com.groupstp.workflowstp.core.constant.*;\n" +
                script;
    }

    protected String substituteParameters(String script) {
        /**
        Matcher matcher = pattern.matcher(script);
        Set<String> found = new HashSet<>();
        while (matcher.find()) {
            found.add(matcher.group());
        }
        if (!CollectionUtils.isEmpty(found)) {
            for (String substitute : found) {
                String value = "context['" + substitute.substring(2).su
                if (value == null) {
                    value = StringUtils.EMPTY;
                }
                script = script.replaceAll(substitute, value);
            }
        }*/
        return script;
    }
}
