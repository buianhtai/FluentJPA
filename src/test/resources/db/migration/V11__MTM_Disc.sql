create schema if not exists MTM;

CREATE TABLE MTM.MEMBERS (
	id INT GENERATED BY DEFAULT AS IDENTITY,
	CHILD_TYPE   CHAR(1)
);

CREATE TABLE MTM.GROUP_MEMBER (
    member_id INT,
    parent_id INT
);

CREATE TABLE MTM.users (
    uid INT,
    name varchar(255)
);