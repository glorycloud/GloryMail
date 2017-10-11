package fortunedog.mail.proxy.exchange;

public interface FolderSyncType
{
	int USER_CREATED = 1;
	int INBOX = 2; //Default Inbox folder
	
	int DRAFTS = 3;//Default Drafts folder
	int DELETED = 4;//Default Deleted Items folder
	int SENT = 5;//Default Sent Items folder
	int OUTBOX=6;//Default Outbox folder
	int TASKS = 7;//Default Tasks folder
	int CALENDAR = 8;//Default Calendar folder
	int CONTACTS = 9;//Default Contacts folder
	int NOTES = 10;//Default Notes folder
	int JOURNAL = 11;//Default Journal folder
	int USER_CREATED_MAIL = 12;//User-created Mail folder
	int USER_CREATED_CALENDAR = 13;//User-created Calendar folder
	int USER_CREATED_CONTACTS = 14;//User-created Contacts folder
	int USER_CREATED_TASKS = 15;//User-created Tasks folder
	int USER_CREATED_JOURNAL = 16;//User-created journal folder
	int USER_CREATED_NOTES = 17;//User-created Notes folder
	int UNKNOWN=18;//Unknown folder type
	int RECIPIENT_INFORMATION = 19;//Recipient information cache
	
	
}
