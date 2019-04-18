package com.groupstp.workflowstp.rest.controller;

import com.groupstp.workflowstp.rest.dto.generic.MessageDTO;
import com.groupstp.workflowstp.rest.dto.WorkflowDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Workflow REST endpoint API
 *
 * @author adiatullin
 */
@RequestMapping(value = "/workflow", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface WorkflowRestAPI {

    @GetMapping(value = "/all")
    List<WorkflowDTO> getWorkflows();

    @PostMapping(value = "/start")
    void start(@RequestParam(name = "id") String entityId,
               @RequestParam(name = "entityName") String entityName);

    @GetMapping(value = "/processing")
    MessageDTO isProcessing(@RequestParam(name = "id") String entityId,
                            @RequestParam(name = "entityName") String entityName);

    @GetMapping(value = "/performable")
    MessageDTO isPerformable(@RequestParam(name = "ids") String[] entityIds,
                             @RequestParam(name = "workflowId") String workflowId,
                             @RequestParam(name = "stageId") String stageId,
                             @RequestParam(name = "actionId") String actionId);

    @PostMapping(value = "/perform")
    MessageDTO perform(@RequestParam(name = "ids") String[] entityIds,
                       @RequestParam(name = "workflowId") String workflowId,
                       @RequestParam(name = "stageId") String stageId,
                       @RequestParam(name = "actionId") String actionId);
}
