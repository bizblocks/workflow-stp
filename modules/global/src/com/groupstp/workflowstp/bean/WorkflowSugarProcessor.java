package com.groupstp.workflowstp.bean;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Special helper class which parse and prepare workflow scripts to support helpful stuff
 *
 * @author adiatullin
 */
@Component(WorkflowSugarProcessor.NAME)
public class WorkflowSugarProcessor {
    public static final String NAME = "wfstp_WorkflowSugarProcessor";

    protected Pattern pattern;
    protected boolean isMiddleware;

    @PostConstruct
    public void init() {
        pattern = Pattern.compile("\\$\\{[^}]+\\}");
        try {
            Class.forName("com.groupstp.workflowstp.core.constant.WorkflowConstants");
            isMiddleware = true;
        } catch (Exception ignore) {
            isMiddleware = false;
        }
    }

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
                "import java.util.*;\n" +
                (isMiddleware ? "import com.groupstp.workflowstp.core.constant.*;\n" : "import com.groupstp.workflowstp.web.util.*;\n") +
                script;
    }

    protected String substituteParameters(String script) {
        Matcher matcher = pattern.matcher(script);
        Set<String> found = new HashSet<>();
        while (matcher.find()) {
            found.add(matcher.group());
        }
        if (!CollectionUtils.isEmpty(found)) {
            for (String substitute : found) {
                String variable = substitute.substring(2);//${
                variable = variable.substring(0, variable.length() - 1);//}
                script = script.replace(substitute, "context['" + variable + "']");
            }
        }
        return script;
    }
}
