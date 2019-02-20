create table WFSTP_TASK_PERFORMERS_LINK (
    TASK_ID uuid,
    PERFORMER_ID uuid,
    primary key (TASK_ID, PERFORMER_ID)
);
