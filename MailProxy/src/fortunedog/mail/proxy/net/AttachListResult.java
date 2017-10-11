package fortunedog.mail.proxy.net;

import java.util.Vector;

import org.jdom.Element;

import fortunedog.mail.proxy.MailPart;
import fortunedog.mail.reflow.PagerFactory;
import fortunedog.util.Utils;
@Deprecated
public class AttachListResult extends Result
{
	private Vector<MailPart> parts;
	@Override
	protected void fillContent(Element content)
	{
		int count = parts.size();
		for(int i=0;i<count;i++)
		{
			try
			{
				MailPart p = parts.get(i);
				String fileName = p.getFileName();
				if(Utils.isEmpty(fileName))
					fileName="Î´ÃüÃû";
				Element m = new Element("attach");
				m.setAttribute("index", i+"");
				m.setAttribute("name", fileName);
				double sizeF = p.getSize();
				String unit = "B";
				if(sizeF > 1024*1024*1024)
				{
					sizeF = sizeF/(1024.*1024*1024);
					unit = "GB";
				}
				else if(sizeF > 1024*1024)
				{
					sizeF = sizeF/(1024.*1024);
					unit = "MB";
				}
				else if(sizeF > 1024)
				{
					sizeF = sizeF/(1024.);
					unit = "KB";
				}
				m.setAttribute("size", String.format("%.3g%s", sizeF, unit));
				m.setAttribute("preview", ""+PagerFactory.canPreview(fileName));
				content.addContent(m);
			}
			finally
			{
				
			}
		}
	}
	
	public AttachListResult(Vector<MailPart> parts)
	{
		this.parts = parts;
	}
}
