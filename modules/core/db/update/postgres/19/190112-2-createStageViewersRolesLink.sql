alter table STAGE_VIEWERS_ROLES_LINK add constraint FK_STAVIEROL_ON_STAGE foreign key (STAGE_ID) references WFSTP_STAGE(ID);
alter table STAGE_VIEWERS_ROLES_LINK add constraint FK_STAVIEROL_ON_ROLE foreign key (VIEWER_ROLE_ID) references SEC_ROLE(ID);
