CREATE TABLE "sysconfig" ("name" VARCHAR(30) PRIMARY KEY  NOT NULL , "value" VARCHAR(100));

CREATE TABLE account (
  name varchar(100) DEFAULT NULL,
  loginName varchar(50) DEFAULT NULL,
  smtpServer varchar(100) DEFAULT NULL,
  mailServer varchar(100) DEFAULT NULL,
  serverType varchar(10) DEFAULT NULL,
  ID INTEGER PRIMARY KEY,
  mailPort smallint(5) DEFAULT '143' ,
  smtpPort smallint(5) DEFAULT '25' ,
  password varchar(50) DEFAULT NULL,
  useSSL tinyint(1) DEFAULT '0'
);

CREATE TABLE "cacheMail" ("accountId" INTEGER NOT NULL, "uidx" INTEGER NOT NULL, "attachIndex" INTEGER, "attachPage" INTEGER, "body" TEXT, "lastAccess" DATETIME, PRIMARY KEY("uidx","accountId")); 
CREATE TABLE "outBox" ("accountId" INTEGER NOT NULL, 
"to" VARCHAR(1024), 
"cc" VARCHAR(1024),
"bc" VARCHAR(512), 
"body" TEXT, 
"refUid" VARCHAR(70), 
"refBody" INTEGER DEFAULT 0,
"subject" VARCHAR(512), 
"attachments" TEXT, 
"date" DATETIME DEFAULT (datetime(CURRENT_TIMESTAMP,'localtime')), 
"folder" VARCHAR(20) NOT NULL, 
"state" INTEGER DEFAULT 1, 
"mailType" INTEGER DEFAULT 1,
"asterisk" INTEGER DEFAULT 0,
ID INTEGER PRIMARY KEY);

CREATE TABLE "attachPreviewCache" ("accountId" INTEGER NOT NULL,
"uidx" INTEGER NOT NULL,
"attachIdx" INTEGER NOT NULL,
"attachPage" INTEGER NOT NULL,
"body"	TEXT,
"lastAccessed" DATETIME DEFAULT CURRENT_TIMESTAMP,
ID INTEGER PRIMARY KEY);


CREATE TABLE "mail" ("uid" varchar(70) NOT NULL ,
"subject" varchar(512),
"date" DATETIME DEFAULT CURRENT_TIMESTAMP,
 "from" varchar(100),
 "state" INTEGER  DEFAULT 0,
 "hasAttach" INTEGER   DEFAULT 0,
 "accountId" INTEGER DEFAULT 0 ,
 "uidx" INTEGER NOT NULL , "to" varchar(1024), "cc" varchar(1024), "folder" VARCHAR(20) DEFAULT INBOX,
 "body" TEXT, "asterisk" INTEGER DEFAULT 0,
 PRIMARY KEY ("uid", "accountId"));

CREATE  TABLE "main"."addressbook" ("name" VARCHAR(20), "email" VARCHAR(60));

CREATE TABLE "folders"("accountid" INTEGER PRIMARY KEY,
"foldername" varchar(100),
"displayname" varchar(100));


CREATE TABLE [attachmentInfo] (
  [mailUid] VARCHAR NOT NULL,
  [attachIdx] INTEGER NOT NULL,
  [fileName] VARCHAR,
  [size] VARCHAR,
  [fileType] VARCHAR,
  [canPreview] tinyint(1) DEFAULT '0',
  [filePath] VARCHAR DEFAULT NULL,
  [accountId] INTEGER NOT NULL,
 PRIMARY KEY ([mailUid], [attachIdx], [accountId]));
