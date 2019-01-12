create table WFSTP_SCREEN_EXTENSION_TEMPLATE (
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
    KEY_ varchar(255) not null,
    ENTITY_NAME varchar(255) not null,
    SCREEN_ID varchar(255) not null,
    IS_BROWSER boolean,
    SCREEN_CONSTRUCTOR text not null,
    --
    primary key (ID)
);
