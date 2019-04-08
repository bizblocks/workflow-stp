package com.groupstp.workflowstp.rest.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workflow REST controller
 *
 * @author adiatullin
 */
@RestController("wfstp_WorkflowRestController")
@RequestMapping(value = "/workflow", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class WorkflowRestController {
}
