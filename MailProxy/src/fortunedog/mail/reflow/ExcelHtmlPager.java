package fortunedog.mail.reflow;

import static net.htmlparser.jericho.HTMLElementName.HEAD;
import static net.htmlparser.jericho.HTMLElementName.LINK;
import static net.htmlparser.jericho.HTMLElementName.META;

import java.util.Map;
import java.util.TreeMap;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.Tag;

/**
 * 
 * 过滤Excel生成的html 
 * 1 删掉script
 * 2 删掉
 * <meta name=ProgId content=Excel.Sheet>
 * <meta name=Generator content="Microsoft Excel 12">
 * <link id=Main-File rel=Main-File href="../账目.htm">
 * <link rel=File-List href=filelist.xml>
 * 保留
 * <meta http-equiv=Content-Type content="text/html; charset=gb2312">
 * <link rel=Stylesheet href=stylesheet.css>
 * @author Daniel
 * 
 */
public class ExcelHtmlPager extends HtmlPager
{

	private static String[][] tagAttributes = new String[][] {
																{
																	HTMLElementName.HTML,
																	"xmlns:*",
																	"xmlns" },
																{
																	HTMLElementName.LINK,
																	"rel",
																	"href" }, };
	private static Map<String, String[]> tagAttrMap = new TreeMap<String, String[]>();
	private String docBase = null;
	static
	{
		for (String[] attr : tagAttributes)
		{
			tagAttrMap.put(attr[0], attr);
		}
	}

	public ExcelHtmlPager(String docBase)
	{	
		super(false);
		this.docBase = docBase;
	}



	@Override
	protected boolean isDeleteTag(String tagName, Tag tag)
	{
		if (tagName == LINK && tag instanceof StartTag)
		{
			Attributes attrs = ((StartTag) tag).getAttributes();
			if (attrs != null)
			{
				Attribute attr = attrs.get("rel");

				if (attr != null && "Stylesheet".equals(attr.getValue()))
				{
					return false; // don't delete this tag, since it is the style
									// sheet reference
				}
				else
					return true; // delete other link tag

			}
		}
		
		if (tagName == META && tag instanceof StartTag)
		{
			Attributes attrs = ((StartTag) tag).getAttributes();
			if (attrs != null)
			{
				Attribute attr = attrs.get("content");

				if (attr != null )
				{
					return false; // don't delete this tag, sinc it is the style
									// sheet reference
				}
				else
					return true; // delete other link tag

			}
			
		}

		

		return false; // keep all tags except above
	}
	protected boolean isSupportedAttribute(String tagName, Attribute attribute)
	{
		if(tagName == LINK || "style".equals(attribute.getName()) || "class".equals(attribute.getName()))
			return true;
		else
			return super.isSupportedAttribute(tagName, attribute);
	}
	
	@Override
	protected void afterAddTag(HtmlPage page, Tag tag)
	{
		if(tag.getName() == HEAD && docBase != null && tag instanceof StartTag)
		{
			page.addTag("<base href=\"" + docBase +"/\"/>");
		}
	}
}
