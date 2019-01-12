alter table WFSTP_WORKFLOW add column CODE varchar(10) ^
update WFSTP_WORKFLOW set CODE = '' where CODE is null ;
alter table WFSTP_WORKFLOW alter column CODE set not null ;
alter table WFSTP_WORKFLOW add column ORDER_ integer ;
