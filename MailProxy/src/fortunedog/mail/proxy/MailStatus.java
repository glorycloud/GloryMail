package fortunedog.mail.proxy;

public interface MailStatus
{

	public static final int MAIL_NEW = 1;
	public static final int MAIL_TO_DEL = 3;//this value is also used in DB store procedure, don't change it's value
	public static final int FLAG_HAS_MORE_PLACEHOLD = 0x8000;
}