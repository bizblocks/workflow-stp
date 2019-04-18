package com.groupstp.workflowstp.rest.controller;

import com.haulmont.cuba.core.app.importexport.EntityImportExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

/**
 * Workflow REST controller
 *
 * @author adiatullin
 */
@RestController("wfstp_WorkflowRestController")
@RequestMapping(value = "/workflow", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class WorkflowRestController {

    @Inject
    private EntityImportExportService entityImportExportService;


    public void getWorkflows() {
        //all active visible to user workflows with stages and permission

    }

    public void start(@RequestParam String id, @RequestParam String entityName) {

    }

    public void start(@RequestParam String id, @RequestParam String entityName, @RequestParam String workflowId) {

    }

    public boolean isProcessing(@RequestParam String id, @RequestParam String entityName) {
        return false;
    }

    public boolean isPerformable(String workflowId, String stageId, String actionId) {
        return false;
    }

    public void perform(String workflowId, String stageId, String actionId) {

    }
}
