package fortunedog.mail.proxy.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;

import sun.misc.BASE64Decoder;
import fortunedog.mail.proxy.net.DataPacket;
import fortunedog.mail.proxy.net.Result;
import fortunedog.util.Utils;

/**
 * 测试：hmtl邮件，简单邮件，附件，内嵌图片，都是否能正确转发
 * @author Daniel
 *
 */
public class SendMail extends DatabaseServlet
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5158813646748117031L;

	@SuppressWarnings("unchecked")
	@Override
	Result dbService(ServiceData d) throws ServletException, IOException, SQLException,
			NeedLoginException
	{
		checkSession(d,true);
		checkMailClient(d);
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload parser = new ServletFileUpload(factory);
		try
		{
			List /* FileItem */ items = parser.parseRequest(d.request);
			Iterator it = items.iterator();
			DataPacket dp = null;
			Map<String, FileItem> attaches = new HashMap<String, FileItem>();
			while(it.hasNext())
			{
				FileItem  item = (FileItem) it.next();
				if("text/_xml_".equals(item.getContentType()) && "__thebody.__".equals(item.getName()))
				{
					dp = parseDataPacket(item.getInputStream());
				}
				else
				{
					attaches.put(item.getName(), item);
				}
			}
			return d.mailClient.sendMail(d.request, d.session, dp, attaches);
		}
		catch (XMLStreamException e)
		{
			throw new RuntimeException(e);
		}
		catch (FileUploadException e)
		{
			throw new RuntimeException(e);
		}
	}

	private DataPacket parseDataPacket(InputStream input) throws FactoryConfigurationError, XMLStreamException,
			IOException
	{
		SMInputFactory inf = new SMInputFactory(XMLInputFactory.newInstance());
		SMHierarchicCursor root = inf.rootElementCursor(input);
		root.advance();
		DataPacket dp = new DataPacket();
		dp.packetType = root.getAttrValue("packetType");
		

		dp.refMailFolder=root.getAttrValue("refMailFolder");
		dp.refMailId = root.getAttrValue("refMailId");
		dp.quoteOld = "true".equals(root.getAttrValue("quoteOld"));
		dp.forwardAttach = "true".equals(root.getAttrValue("forwardAttach"));

		SMInputCursor cursor = root.childCursor();
		SMEvent evt;
		while((evt = cursor.getNext()) != null)
		{
//			System.out.println("Event="+evt);
			if(evt == SMEvent.START_ELEMENT)
			{
				String name = cursor.getLocalName();
				System.out.println(name);
				if(name.equals("toList"))
				{
					dp.toList = cursor.collectDescendantText();
				}
				else if(name.equals("ccList"))
				{
					dp.ccList = cursor.collectDescendantText();
				}
				else if(name.equals("bccList"))
				{
					dp.bccList = cursor.collectDescendantText();
				}
				else if(name.equals("subject"))
				{
					dp.subject = cursor.collectDescendantText();
				}
				else if(name.equals("bodyText"))
				{
					dp.bodyText = cursor.collectDescendantText();
				}
				else if(name.equals("attachments"))
				{
					SMInputCursor attachC = cursor.childElementCursor("attachmentInfo");
					
					SMEvent t;
					while((t = attachC.getNext() )!= null)
					{
						AttachmentInfo info = new AttachmentInfo();
						SMInputCursor attachCursor = attachC.childCursor();
						SMEvent t2 ;
						while((t2 = attachCursor.getNext()) != null)
						{
							String n = attachCursor.getLocalName();
							if(t2 == SMEvent.START_ELEMENT)
							{
								if("fileName".equals(n))
								{
									info.fileName = attachCursor.collectDescendantText();
								}
								else if("index".equals(n))
								{
									info.index = Integer.parseInt(attachCursor.collectDescendantText());
								}
								else if("body".equals(n))
								{
									String body = attachCursor.collectDescendantText(true) ;
									if(!Utils.isEmpty(body))
									{
										sun.misc.BASE64Decoder dec = new BASE64Decoder();
										info.body = dec.decodeBuffer(body);
									}
								}
							}
						}
						
						dp.attachments.add(info);
						
					}
				}
			
				
			}
			
			
		}
		return dp;
	}

}
