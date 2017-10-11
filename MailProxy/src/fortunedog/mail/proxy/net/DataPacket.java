package fortunedog.mail.proxy.net;

import java.util.Vector;

import fortunedog.mail.proxy.servlet.AttachmentInfo;


public class DataPacket
{
	public static final String NEWMAIL_TYPE = "newMail";
	public static final String REPLYMAIL_TYPE = "replyMail";
	public static final String FORWARDMAIL_TYPE = "forwardMail";

	public String packetType = null;
	public String subject = null;
	public String toList = null;
	public String ccList = null;
	public String bccList;
	public String bodyText = null;
	public String refMailId = null;
	public String refMailFolder=null;
	public boolean quoteOld = false;
	public boolean forwardAttach = false;
	public Vector<AttachmentInfo> attachments = new Vector<AttachmentInfo>();
}