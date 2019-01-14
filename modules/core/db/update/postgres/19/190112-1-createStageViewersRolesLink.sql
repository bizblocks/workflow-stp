create table STAGE_VIEWERS_ROLES_LINK (
    STAGE_ID uuid,
    VIEWER_ROLE_ID uuid,
    primary key (STAGE_ID, VIEWER_ROLE_ID)
);
