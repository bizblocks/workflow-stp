insert into WFSTP_TASK_PERFORMERS_LINK (TASK_ID, PERFORMER_ID)
select ID, PERFORMER_ID from WFSTP_WORKFLOW_INSTANCE_TASK
where PERFORMER_ID is not null;