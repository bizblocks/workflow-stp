create table WFSTP_SCREEN_TABLE_COLUMN_TEMPLATE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    ENTITY_NAME varchar(255),
    CAPTION varchar(255) not null,
    COLUMN_ID varchar(255) not null,
    GENERATOR_SCRIPT text not null,
    EDITABLE boolean,
    --
    primary key (ID)
);
