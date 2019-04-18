package com.groupstp.workflowstp.rest.controller;

import com.groupstp.workflowstp.rest.dto.ResponseDTO;
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

    /**
     * Get all active workflows from system
     *
     * @return all visible to user and ready to use workflows
     */
    @GetMapping(value = "/all")
    List<WorkflowDTO> getWorkflows();

    /**
     * Start workflow process
     *
     * @param entityId   processing entity id
     * @param entityName processing entity name (like bo$Entity)
     */
    @PostMapping(value = "/start")
    void start(@RequestParam(name = "id") String entityId,
               @RequestParam(name = "entityName") String entityName);

    /**
     * Check is send entity in workflow process or not
     *
     * @param entityId   processing entity id
     * @param entityName processing entity name (like bo$Entity)
     * @return is provided entity in workflow process or not
     */
    @GetMapping(value = "/processing")
    ResponseDTO<Boolean> isProcessing(@RequestParam(name = "id") String entityId,
                                      @RequestParam(name = "entityName") String entityName);

    /**
     * Check can current user perform workflow action of the specified entities
     *
     * @param entityIds  performing entities ids
     * @param workflowId current processing workflow id
     * @param stageId    processing stage id
     * @param actionId   performing action id
     * @return is provided user can perform action to the specified entities or not
     */
    @GetMapping(value = "/performable")
    ResponseDTO<Boolean> isPerformable(@RequestParam(name = "ids") String[] entityIds,
                                       @RequestParam(name = "workflowId") String workflowId,
                                       @RequestParam(name = "stageId") String stageId,
                                       @RequestParam(name = "actionId") String actionId);

    /**
     * Perform provided action with current user for specified entities
     *
     * @param entityIds  performing entities ids
     * @param workflowId current processing workflow id
     * @param stageId    processing stage id
     * @param actionId   performing action id
     * @return result of the performing
     */
    @PostMapping(value = "/perform")
    ResponseDTO<String> perform(@RequestParam(name = "ids") String[] entityIds,
                                @RequestParam(name = "workflowId") String workflowId,
                                @RequestParam(name = "stageId") String stageId,
                                @RequestParam(name = "actionId") String actionId);
}
