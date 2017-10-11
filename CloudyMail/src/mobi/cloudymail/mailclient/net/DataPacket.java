package mobi.cloudymail.mailclient.net;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;



public class DataPacket 
{
	private static final long serialVersionUID = -8601295852655158891L;
	//type in mail server.
	public static final String NEWMAIL_TYPE = "newMail";
	public static final String REPLYMAIL_TYPE = "replyMail";
	public static final String FORWARDMAIL_TYPE = "forwardMail";

	@Attribute
	public String packetType = NEWMAIL_TYPE;
	@Element
	public String subject = null;
	@Element
	public String toList = null;
	@Element
	public String ccList = null;
	@Element
	public String bccList = null;
	@Element
	public String bodyText = null;
	@Attribute
	public String refMailId = "";
	@Attribute
	public String refMailFolder="";
	@Attribute
	public boolean quoteOld = false;
	@Attribute
	public boolean forwardAttach = false;
	@ElementList(type=AttachmentInfo.class)
	public List<AttachmentInfo> attachments;
}