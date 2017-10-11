/*
SQLyog Enterprise - MySQL GUI v8.14 
MySQL - 5.1.43-community : Database - test
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`test` /*!40100 DEFAULT CHARACTER SET latin1 */;

USE `test`;

/*Table structure for table `account` */

DROP TABLE IF EXISTS `account`;

CREATE TABLE `account` (
  `name` varchar(100) DEFAULT NULL COMMENT 'complete email address',
  `loginName` varchar(50) DEFAULT NULL COMMENT 'login name of this account',
  `smtpServer` varchar(100) DEFAULT NULL COMMENT 'domain name or IP of smtp server',
  `mailServer` varchar(100) DEFAULT NULL,
  `serverType` enum('pop3','imap') DEFAULT NULL COMMENT '1 for pop3, 2 for imap',
  `ID` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID for this account, reference by other tables',
  `password` varchar(50) DEFAULT NULL COMMENT 'user''s password',
  `mailPort` smallint(5) unsigned DEFAULT '110' COMMENT 'port number of recive server, i.e. pop3, imap, or pop3 SSL',
  `smtpPort` smallint(5) unsigned DEFAULT '25' COMMENT 'port number of send server, i.e. smtp port',
  `useSSL` tinyint(1) DEFAULT '0',
  `lastAcitve` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `mailIndexCounter` int(10) unsigned DEFAULT '1',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `indexByEmallName` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Table structure for table `dumy1` */

DROP TABLE IF EXISTS `dumy1`;

CREATE TABLE `dumy1` (
  `a` char(10) DEFAULT NULL,
  `b` decimal(6,0) NOT NULL,
  PRIMARY KEY (`b`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Table structure for table `mails` */

DROP TABLE IF EXISTS `mails`;

CREATE TABLE `mails` (
  `uid` varchar(70) NOT NULL COMMENT 'message id from UIDL',
  `accountId` decimal(6,0),
  `subject` varchar(512) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `from` varchar(100) DEFAULT NULL,
  `to` varchar(512) DEFAULT NULL,
  `cc` varchar(512) DEFAULT NULL,
  `state` tinyint(1) unsigned DEFAULT NULL COMMENT 'mark mail as new(1), old(2), to_del(3)',
  `index` int(11) DEFAULT NULL,
  `uidx` int unsigned,
  `attachmentFlag` tinyint(1) unsigned DEFAULT 0,
  `previewContent` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`uid`, `accountId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Table structure for table `knownservers` */

DROP TABLE IF EXISTS `knownservers`;

CREATE TABLE `knownservers` (
  `domainName` varchar(100) NOT NULL,
  `loginName` varchar(100) DEFAULT NULL,
  `mailServer` varchar(100) DEFAULT NULL,
  `smtpServer` varchar(100) DEFAULT NULL,
  `serverType` varchar(10) DEFAULT NULL,
  `smtpPort` varchar(10) DEFAULT NULL,
  `useSSL` varchar(10) DEFAULT NULL,
  `mailPort` varchar(10) DEFAULT NULL,
  `enterprise` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`domainName`,`enterprise`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `knownservers` */

insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('126.com','$USER','pop3.126.com','smtp.126.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('139.com','$USER','pop.139.com','smtp.139.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('163.com','$USER','pop.163.com','smtp.163.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('188.com','$USER','pop.188.com','smtp.188.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('21cn.com','$USER','pop.21cn.com','smtp.21cn.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('21cn.net','$USER','pop.21cn.net','smtp.21cn.net','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('263.net','$USER','263.net','smtp.263.net','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('263.net.cn','$USER','263.net.cn','263.net.cn','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('263xmail.com','$USER','pop.263xmail.com','smtp.263xmail.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('china.com','$USER@china.com','pop.china.com','smtp.china.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('ee.buaa.edu.cn','$USER@ee.buaa.edu.cn','pop3.buaa.edu.cn','smtp.buaa.edu.cn','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('eyou.com','$USER@eyou.com','pop3.eyou.com','mx.eyou.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('foxmail.com','$USER@foxmail.com','pop.qq.com','smtp.qq.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('gmail.com','$USER@gmail.com','pop.gmail.com','smtp.gmail.com','pop3','465','true','995');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('hotmail.com','$USER@hotmail.com','pop3.live.com','smtp.live.com','pop3','587','true','995');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('netease.com','$USER','pop.netease.com','smtp.netease.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('qq.com','$USER','pop.qq.com','smtp.qq.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('sina.cn','$USER','pop.sina.com','smtp.sina.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('sina.com','$USER','pop.sina.com','smtp.sina.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('sohu.com','$USER','pop3.sohu.com','smtp.sohu.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('tom.com','$USER','pop.tom.com','smtp.tom.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('vip.163.com','$USER','pop.vip.163.com','smtp.vip.163.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('vip.qq.com','$USER@vip.qq.com','pop.qq.com','smtp.qq.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('vip.sina.cn','$USER','pop3.vip.sina.com','smtp.vip.sina.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('vip.sohu.com','$USER','pop3.vip.sohu.com','smtp.vip.sohu.com','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('x263.net','$USER','pop.x263.net','smtp.x263.net','pop3','25','false','110');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('yahoo.cn','$USER@yahoo.cn','pop.mail.yahoo.cn','smtp.mail.yahoo.cn','pop3','465','true','995');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('yahoo.com','$USER@yahoo.com','pop3.mail.yahoo.com','smtp.mail.yahoo.com','pop3','465','true','995');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('yahoo.com.cn','$USER@yahoo.com.cn','pop.mail.yahoo.com.cn','smtp.mail.yahoo.cn','pop3','465','true','995');
insert  into `knownservers`(`domainName`,`loginName`,`mailServer`,`smtpServer`,`serverType`,`smtpPort`,`useSSL`,`mailPort`) values ('yeah.net','$USER','pop.yeah.net','smtp.yeah.net','pop3','25','false','110');

insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values('google.com','$USER@$DOMAIN','imap.gmail.com','smtp.gmail.com','imap','465','true','993','1');
insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values('qiye.163.com','$USER@$DOMAIN','imap.qiye.163.com','smtp.qiye.163.coom','imap','994','true','993','1');
insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values('263xmail.com','$USER@$DOMAIN','popcom.263xmail.com','smtpcom.263xmail.com','pop3','25','false','110','1');
insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values('sina.net','$USER@$DOMAIN','pop.sina.net','smtp.sina.net','pop3','465','true','995','1');
insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values('sinanet.com','$USER@$DOMAIN','pop.sinanet.com','smtp.sinanet.com','pop3','465','true','995','1');
insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values('sohu.net','$USER@$DOMAIN','pop3.sohu.net','smtp.sohu.net','pop3','25','false','110','1');
insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values('21cn.com','$USER@$DOMAIN','imap-ent.21cn.com','smtp-ent.21cn.com','imap','465','true','993','1');
insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values ('hotmail.com','$USER@$DOMAIN','pop3.live.com','smtp.live.com','pop3','587','true','995','1');
insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values ('qq.com','$USER@$DOMAIN','imap.exmail.qq.com','smtp.exmail.qq.com','pop3','465','true','993','1');
insert into knownservers (domainName,loginName,mailServer,smtpServer,serverType,smtpPort,useSSL,mailPort,enterprise) values ('zoho.com', '$USER@$DOMAIN', 'imap.zoho.com', 'smtp.zoho.com', 'imap', 465, true, 993, 1);
insert into knownservers (domainName,loginName,mailServer,smtpServer,serverType,smtpPort,useSSL,mailPort,enterprise) values ('ojooo.com', '$USER@ojooo.com', 'mail.ojooo.com', 'mail.ojooo.com', 'imap', 465, true, 993, 0);
insert into knownservers (domainName,loginName,mailServer,smtpServer,serverType,smtpPort,useSSL,mailPort,enterprise) values ('ojooo.com', '$USER@$DOMAIN', 'mail.ojooo.com', 'mail.ojooo.com', 'imap', 465, true, 993, 1);
insert into `knownservers` (`domainName`, `loginName`, `mailServer`, `smtpServer`, `serverType`, `smtpPort`, `useSSL`, `mailPort`,`enterprise`) values ('outlook.com','$USER@outlook.com','pop3.live.com','smtp.live.com','pop3','587','true','995',0);
 
/* Function  structure for function  `check_mail` */

/*!50003 DROP FUNCTION IF EXISTS `check_mail` */;
DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` FUNCTION `check_mail`(mail_account decimal(6,0), mail_uid varchar(70), mail_idx INT) RETURNS int(11)
BEGIN
	update mails set state = 2, `index`=mail_idx where uid=mails_uid;
	IF row_count() = 0 THEN 
		INSERT INTO mails (uid, accountId, `index`, state) VALUES(mail_uid, mail_account, mail_idx, 1);
		RETURN 1;
	ELSE
		RETURN 2;
	END IF;
    END */$$
DELIMITER ;

/* Function  structure for function  `test_f` */

/*!50003 DROP FUNCTION IF EXISTS `test_f` */;
DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` FUNCTION `test_f`(uid varchar(10) ) RETURNS int(11)
BEGIN
	UPDATE dumy1 SET a='cc' WHERE b = uid;
	IF row_count() = 0 THEN 
		insert into dumy1(a, b) values('nn', uid);
		return 1;
	ELSE
		RETURN 2;
	END IF;
    END */$$
DELIMITER ;

/* Procedure structure for procedure `test` */

/*!50003 DROP PROCEDURE IF EXISTS  `test` */;

DELIMITER $$

/*!50003 CREATE DEFINER=`root`@`localhost` PROCEDURE `test`(in uid varchar(70), out state int )
BEGIN
	declare a_v varchar(10);
	select a into a_v from dumy1 where b=uid;
	if ISNULL(a_v) then 
		insert into dumy1 values('new', uid);
		set state = 1;
	else
		set state = 2;
	end if;
    END */$$
DELIMITER ;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;



DELIMITER $$

USE `test`$$

DROP FUNCTION IF EXISTS `updateMail`$$

CREATE DEFINER=`root`@`localhost` FUNCTION `updateMail`(p_subject VARCHAR(512), p_date DATETIME, p_from VARCHAR(100), p_state INT, p_uid VARCHAR(70), aId INT) RETURNS INT(11)
BEGIN
	

	DECLARE cnt INT  UNSIGNED ;
	SELECT mailIndexCounter INTO cnt FROM account WHERE ID=aId FOR UPDATE;
	UPDATE account SET mailIndexCounter=cnt+1 WHERE ID=aId;
	UPDATE mails SET `subject`=p_subject, `date`=p_date, `from`=p_from, `state`=p_state, uidx=cnt WHERE accountId=aId AND uid=p_uid;
	
	RETURN @cnt;
    END$$

DELIMITER ;


DELIMITER $$

USE `test`$$

DROP FUNCTION IF EXISTS `reassignMailUidx`$$

CREATE DEFINER=`root`@`localhost` FUNCTION `reassignMailUidx`(aId INT, folder varchar(50)) RETURNS INT(11)
BEGIN
	
	DECLARE p_uid VARCHAR(70);
	DECLARE done INT DEFAULT 0;
	DECLARE stopFlag INT;
	DECLARE cnt INT  UNSIGNED ;
	DECLARE readCursor CURSOR
		FOR SELECT uid FROM mails WHERE uidx IS NULL AND accountId=aId AND state=3 FOR UPDATE;  /* update deleted mail's uidx*/
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
