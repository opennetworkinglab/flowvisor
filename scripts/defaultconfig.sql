DELETE FROM jFSRSlice;
ALTER TABLE jFSRSlice ALTER COLUMN id RESTART WITH 1;

DELETE FROM Slice;
ALTER TABLE Slice ALTER COLUMN id RESTART WITH 1;

DELETE FROM FlowSpaceRule;
ALTER TABLE FlowSpaceRule ALTER COLUMN id RESTART WITH 1;

DELETE FROM Flowvisor;
ALTER TABLE Flowvisor ALTER COLUMN id RESTART WITH 1;

INSERT INTO Flowvisor(config_name) VALUES('default');

INSERT INTO Slice(flowvisor_id, flowmap_type, name, creator, passwd_crypt, passwd_salt, controller_hostname, controller_port, contact_email) VALUES(1, 1, 'fvadmin', 'fvadmin', 'CHANGEME', 'sillysalt', 'none', 0, 'fvadmin@localhost');
INSERT INTO Slice(flowvisor_id, flowmap_type, name, creator, passwd_crypt, passwd_salt, controller_hostname, controller_port, contact_email) VALUES(1, 1, 'alice', 'fvadmin', 'alicePass', 'sillysalt', 'localhost', 54321, 'alice@foo.com');
INSERT INTO Slice(flowvisor_id, flowmap_type, name, creator, passwd_crypt, passwd_salt, controller_hostname, controller_port, contact_email) VALUES(1, 1, 'bob', 'fvadmin', 'bobPass', 'sillysalt', 'localhost', 54322, 'bob@foo.com');

/* alice rules ids 1 to 6 resp.*/
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 0, 2);
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 2, 2);
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 3, 2);
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 0, 4294967298);
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 2, 4294967298);
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 3, 4294967298);

/* bob rules ids 7 - 10 resp*/
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 1, 1);
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 3, 1);
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 1, 4294967297);
INSERT INTO FlowSpaceRule(dpid, priority, in_port, dl_src) VALUES(-9223372036854775808, 32000, 3, 4294967297);

INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(1,2,4);
INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(2,2,4);
INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(3,2,4);
INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(4,2,4);
INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(5,2,4);
INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(6,2,4);

INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(7,3,4);
INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(8,3,4);
INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(9,3,4);
INSERT INTO jFSRSlice(flowspacerule_id, slice_id, slice_action) VALUES(10,3,4);
