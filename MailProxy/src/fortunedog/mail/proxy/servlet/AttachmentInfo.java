package fortunedog.mail.proxy.servlet;

public class AttachmentInfo
{
	public final static int LOCAL_ATTACH_INDEX = -1;
	public final static int ALL_REFATTACH_INDEX = -2;
	public String fileName;
	public int index = -1; //index of this attachment in its container mail, -1 means its from local
	public byte[] body; //body of the attachment, used to upload local created attachment to server
}