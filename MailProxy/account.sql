/*
SQLyog Enterprise - MySQL GUI v8.14 
MySQL - 5.1.44-community : Database - test
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
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
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `lastActive` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `mailIndexCounter` int(10) unsigned DEFAULT '1',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `indexByEmallName` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=latin1;

/*Data for the table `account` */

insert  into `account`(`name`,`loginName`,`smtpServer`,`mailServer`,`serverType`,`ID`,`password`,`mailPort`,`smtpPort`,`useSSL`,`createDate`,`mailIndexCounter`) values ('liu_lele@126.com','liu_lele','smtp.126.com','pop3.126.com','pop3',7,NULL,110,25,0,'2010-04-25 23:31:42',8436);
insert  into `account`(`name`,`loginName`,`smtpServer`,`mailServer`,`serverType`,`ID`,`password`,`mailPort`,`smtpPort`,`useSSL`,`createDate`,`mailIndexCounter`) values ('liulele@ee.buaa.edu.cn','cocalele@gmail.com','smtp.gmail.com','pop.gmail.com','pop3',8,NULL,143,465,1,'2010-07-16 22:57:31',1);
insert  into `account`(`name`,`loginName`,`smtpServer`,`mailServer`,`serverType`,`ID`,`password`,`mailPort`,`smtpPort`,`useSSL`,`createDate`,`mailIndexCounter`) values ('cocalele@gmail.com','cocalele@gmail.com','smtp.gmail.com','pop.gmail.com','pop3',9,NULL,995,465,1,'2010-08-30 22:20:43',1);

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
