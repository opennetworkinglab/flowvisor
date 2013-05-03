CONNECT 'jdbc:derby:FlowVisorDB;create=true';

AUTOCOMMIT off;

CREATE TABLE Flowvisor (
  id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  api_webserver_port INT DEFAULT 8080 ,
  api_jetty_webserver_port INT DEFAULT 8081 ,
  checkpointing BOOLEAN DEFAULT false, 
  listen_port INT  DEFAULT 6633 ,
  track_flows BOOLEAN   DEFAULT false ,
  stats_desc_hack BOOLEAN  DEFAULT false ,
  run_topology_server BOOLEAN DEFAULT false ,
  logging VARCHAR(45)  DEFAULT 'NOTE' ,
  log_ident VARCHAR(45)  DEFAULT 'flowvisor' ,
  log_facility VARCHAR(45)  DEFAULT 'LOG_LOCAL7' ,
  version VARCHAR(45)  DEFAULT 'flowvisor-0.8.5' ,
  host VARCHAR(45)  DEFAULT 'localhost' ,
  default_flood_perm VARCHAR(45) DEFAULT 'fvadmin',
  config_name VARCHAR(100) UNIQUE DEFAULT 'default',
  db_version INT,
  fscache INT DEFAULT 30,
  PRIMARY KEY (id));

CREATE TABLE Slice (
  id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  flowvisor_id INT NOT NULL,
  flowmap_type INT NOT NULL,
  name VARCHAR(45) NOT NULL UNIQUE,
  creator VARCHAR(45) NOT NULL ,
  passwd_crypt VARCHAR(35) ,
  passwd_salt VARCHAR(45) ,
  controller_hostname VARCHAR(45) NOT NULL ,
  controller_port INT NOT NULL ,
  contact_email VARCHAR(150) NOT NULL ,
  drop_policy VARCHAR(10) DEFAULT 'exact' ,
  lldp_spam BOOLEAN  DEFAULT true,
  max_flow_rules INT NOT NULL DEFAULT -1,
  admin_status BOOLEAN DEFAULT TRUE,
  PRIMARY KEY (id));

CREATE INDEX flowvisor_index ON Slice (flowvisor_id ASC);

ALTER TABLE Slice
	ADD CONSTRAINT flowvisor_instance FOREIGN KEY (
		flowvisor_id)
	REFERENCES
		FlowVisor (id) ON DELETE CASCADE;


CREATE TABLE jFSRSlice (
	id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	flowspacerule_id INT,
	slice_id INT,
	slice_action INT,
	PRIMARY KEY (id));

CREATE INDEX flowspace_index ON jFSRSlice (flowspacerule_id ASC);
CREATE INDEX slice_index ON jFSRSlice (slice_id ASC);

CREATE TABLE FSRQueue (
  id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  fsr_id INT NOT NULL,
  queue_id INT DEFAULT -1,
  PRIMARY KEY (id));

CREATE INDEX fsrqueue_index on FSRQueue (fsr_id ASC);

CREATE TABLE FlowSpaceRule (
  id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  dpid BIGINT, 
  priority INT NOT NULL,
  in_port SMALLINT,
  dl_vlan SMALLINT,
  dl_vpcp SMALLINT,
  dl_src BIGINT,
  dl_dst BIGINT, 
  dl_type SMALLINT,
  nw_src INT,
  nw_dst INT,
  nw_proto SMALLINT,
  nw_tos SMALLINT,
  tp_src SMALLINT,
  tp_dst SMALLINT,
  forced_queue INT DEFAULT -1,
  wildcards INT,
  name VARCHAR(64),
  PRIMARY KEY (id));

CREATE INDEX prio_index ON FlowSpaceRule (priority ASC);

ALTER TABLE FSRQueue
    ADD CONSTRAINT FlowSpaceRule_to_queue_fk FOREIGN KEY (fsr_id)
    REFERENCES FlowSpaceRule (id) ON DELETE CASCADE;

ALTER TABLE jFSRSlice 
	ADD CONSTRAINT FlowSpaceRule_to_Slice_fk FOREIGN KEY (slice_id)
	REFERENCES Slice (id) ON DELETE CASCADE;

ALTER TABLE jFSRSlice 
	ADD CONSTRAINT Slice_to_FlowSpaceRule_fk FOREIGN KEY (flowspacerule_id)
	REFERENCES FlowSpaceRule (id) ON DELETE CASCADE;



CREATE TABLE Switch (
  id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  dpid BIGINT ,
  flood_perm VARCHAR(45) ,
  mfr_desc VARCHAR(256) ,
  hw_desc VARCHAR(256) ,
  sw_desc VARCHAR(256) ,
  serial_num VARCHAR(32) ,
  dp_desc VARCHAR(256) ,
  capabilities INT,
  PRIMARY KEY (id) );


CREATE TABLE jSliceSwitchLimits (
    id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    slice_id INT NOT NULL,
    switch_id INT NOT NULL,
    maximum_flow_mods INT NOT NULL DEFAULT -1,
    rate_limit INT NOT NULL DEFAULT -1,
    PRIMARY KEY (id));

CREATE INDEX slice_limit_index ON jSliceSwitchLimits (slice_id ASC);
CREATE INDEX switch_limit_index ON jSliceSwitchLimits (switch_id ASC);

ALTER TABLE JSliceSwitchLimits
    ADD CONSTRAINT limit_to_switch_fk FOREIGN KEY (switch_id)
    REFERENCES Switch (id) ON DELETE CASCADE;

ALTER TABLE jSliceSwitchLimits
    ADD CONSTRAINT limit_to_slice_fk FOREIGN KEY (slice_id)
    REFERENCES Slice (id) ON DELETE CASCADE;

CREATE TABLE jFSRSwitch (
	id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
	flowspacerule_id INT,
	switch_id INT,
	PRIMARY KEY (id));

CREATE INDEX fs_index ON jFSRSwitch (flowspacerule_id ASC);
CREATE INDEX switch_index ON jFSRSwitch (switch_id ASC);

ALTER TABLE jFSRSwitch
	ADD CONSTRAINT FlowSpaceRule_to_Switch_fk FOREIGN KEY (switch_id)
	REFERENCES Switch (id);

ALTER TABLE jFSRSwitch
	ADD CONSTRAINT Switch_to_FlowSpaceRule_fk FOREIGN KEY (flowspacerule_id)
	REFERENCES FlowSpaceRule (id);

CREATE TABLE FlowTableEntry (
  id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
  switch_id INT NOT NULL,
  slice_id INT NOT NULL,
  cookie_id INT DEFAULT -1,
  parent_id INT DEFAULT -1,
  priority INT  NOT NULL,
  in_port INT,
  dl_vlan SMALLINT,
  dl_vpcp SMALLINT,
  dl_src BIGINT,
  dl_dst BIGINT, 
  dl_type SMALLINT,
  nw_src INT,
  nw_dst INT,
  nw_proto SMALLINT,
  nw_tos SMALLINT,
  tp_src SMALLINT,
  tp_dst SMALLINT,
  PRIMARY KEY (id));

CREATE INDEX flowentry_switch_index ON FlowTableEntry (switch_id ASC);

ALTER TABLE FlowTableEntry 
	ADD CONSTRAINT flowentry_switch_fk FOREIGN KEY (switch_id)
	REFERENCES Switch (id);


CREATE TABLE Actions (
  id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
  flowentry_id INT ,
  type SMALLINT ,
  length SMALLINT,
  value BIGINT ,
  PRIMARY KEY (id));

CREATE INDEX action_flowentry_index ON Actions (flowentry_id ASC);

ALTER TABLE Actions
	ADD CONSTRAINT actions_flowentry_fk FOREIGN KEY (flowentry_id)
	REFERENCES FlowTableEntry (id);

CREATE TABLE Cookie (
    id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    controller_cookie BIGINT,
    fv_cookie BIGINT,
    PRIMARY KEY(id));

ALTER TABLE FlowTableEntry
    ADD CONSTRAINT flowentry_cookie_fk FOREIGN KEY (cookie_id)
    REFERENCES Cookie (id);

COMMIT;
		     
