create table WFSTP_WORKFLOW_DEFINITION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ENTITY_NAME varchar(255) not null,
    WORKFLOW_ID uuid not null,
    PRIORITY_ integer not null,
    CONDITION_SQL_SCRIPT text,
    CONDITION_XML text,
    CONDITION_GROOVY_SCRIPT text,
    --
    primary key (ID)
);
