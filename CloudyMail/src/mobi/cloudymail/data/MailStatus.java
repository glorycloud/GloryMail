package mobi.cloudymail.data;

public interface MailStatus
{
	public static final int MAIL_NEW = 1;
	public static final int MAIL_READED = 2;
	public static final int MAIL_DELETED = 3;
	public static final int MAIL_DELETE_FOREVER = 4;
	public static final int MAIL_LOCAL_DELETED = 5;
	public static final int MAIL_INVALID_STATE = -1;
	public static final int FLAG_HAS_MORE_PLACEHOLD = 0x8000;
}
