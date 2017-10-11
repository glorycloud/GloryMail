package fortunedog.mail.proxy.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;

import fortunedog.mail.proxy.StructurizedMail;
import fortunedog.mail.proxy.net.AttachListResult;
import fortunedog.mail.proxy.net.Result;

/**
 * This servlete is no more used by client
 * @author Daniel
 *
 */
@Deprecated
public class AttachList extends DatabaseServlet
{
	private static final long serialVersionUID = -3603159040761574907L;
	@Override
	Result dbService(ServiceData d)
		throws ServletException, IOException, SQLException, NeedLoginException
	{
		
		checkSession(d,true);
		checkMailClient(d);
		String uid = d.request.getParameter("uid");
		String folderName=new String(d.request.getParameter("folderName").getBytes("ISO8859-1"),"UTF-8");
		StructurizedMail mail = d.mailClient.getMail(d.session,uid,folderName);
		return new AttachListResult(mail.attachmentParts);

	}
}
