/* by tim liu, change account's column lastAcitve to createTime, and add column lastActive */
alter table account change lastAcitve createTime timestamp NULL DEFAULT CURRENT_TIMESTAMP;
alter table account add lastActive timestamp;
update account set lastActive=createTime;
alter table account add lastFailedLogin timestamp;

DROP TABLE IF EXISTS `folders`;

CREATE TABLE `folders` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `accountid` decimal(10,0) NOT NULL DEFAULT '0',
  `foldername` varchar(50) NOT NULL,
  `displayname` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `accountid` (`accountid`,`foldername`)
) ENGINE=InnoDB AUTO_INCREMENT=6104 DEFAULT CHARSET=utf8;

alter table mails add foldername varchar(50) not null default 'INBOX';
ALTER TABLE mails DROP PRIMARY KEY, ADD PRIMARY KEY(uid,accountId,foldername);

DELIMITER $$

USE `test`$$

DROP FUNCTION IF EXISTS `reassignMailUidx2`$$

CREATE DEFINER=`root`@`localhost` FUNCTION `reassignMailUidx2`(aId INT, folder VARCHAR(50)) RETURNS INT(11)
BEGIN
	
	DECLARE p_uid VARCHAR(70);
	DECLARE done INT DEFAULT 0;
	DECLARE stopFlag INT;
	DECLARE cnt INT  UNSIGNED ;
	DECLARE readCursor CURSOR
		FOR SELECT uid FROM mails WHERE uidx IS NULL AND accountId=aId AND state=3 FOR UPDATE;  
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
	DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;
	OPEN readCursor;
	SELECT mailIndexCounter INTO cnt FROM account WHERE ID=aId FOR UPDATE;
	
	read_loop: LOOP
	
		FETCH readCursor INTO p_uid;
		IF done THEN
			LEAVE read_loop;
		END IF;
	
		UPDATE mails SET uidx=cnt WHERE uid=p_uid AND accountId=aId AND state=3 AND foldername=folder;
		SET cnt = cnt+1;
	
	END LOOP;
	
	CLOSE readCursor;
	UPDATE account SET mailIndexCounter=cnt WHERE ID=aId;
	RETURN @cnt;
    END$$

DELIMITER ;

alter table account add column fileName varchar(256);

/* to support exchange, after 1.4.3 releas*/
alter table account modify serverType enum('pop3', 'imap', 'exchange');
alter table account add column syncKey varchar(64) default '0';
alter table folders add column parentId varchar(64);
alter table folders add column serverId varchar(64);
alter table folders add syncKey varchar(64) default '0';
DROP FUNCTION IF EXISTS nextUidx;
DELIMITER $
CREATE FUNCTION nextUidx (accountId int(10) unsigned)
RETURNS int(10) unsigned
CONTAINS SQL
BEGIN
   DECLARE value int(10) unsigned;
   UPDATE account
   SET          mailIndexCounter = mailIndexCounter + 1
   WHERE id = accountId;
   SET value = 0;  
   SELECT mailIndexCounter INTO value  
   		FROM account  
   		WHERE id = accountId; 
   RETURN value;  
END$
DELIMITER ;

