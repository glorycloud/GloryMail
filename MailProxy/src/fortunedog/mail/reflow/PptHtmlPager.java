package fortunedog.mail.reflow;

import static net.htmlparser.jericho.HTMLElementName.BODY;
import static net.htmlparser.jericho.HTMLElementName.HEAD;
import static net.htmlparser.jericho.HTMLElementName.LINK;

import java.util.Map;
import java.util.TreeMap;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.Tag;

/**
 * 
 * 过滤PowerPoint生成的html 1, 把<div id=SlideObj 里面的visibility:hidden去掉, 保证可以显示 2,
 * 把脚本script都删掉 3. v:shape 等，所有标签的o:gfxdata属性删掉 ppt html,去掉<script
 * src="script.js"> ,script.js是一个很庞大的文件 删掉 <link id=Main-File rel=Main-File
 * href="../test2.htm"> <link rel=Preview href=preview.wmf> 删掉p:标签,删掉o:标签
 * 
 * @author Daniel
 * 
 */
public class PptHtmlPager extends HtmlPager
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

	public PptHtmlPager(String docBase)
	{	
		super(false);
		this.docBase = docBase;
	}

	// for v:shape, delete
	@Override
	protected boolean isSupportedAttribute(String tagName, Attribute attribute)
	{
		String attrName = attribute.getName().toLowerCase();
		if (attrName.equals("o:gfxdata"))
			return false; // delete gfxdata attribute
		if(tagName == BODY && attribute.getKey().equals("onload"))
		{
			attribute.setValue("window.cmail.fitView();docload();");
		}
		return true;
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
					return false; // don't delete this tag, sinc it is the style
									// sheet reference
				}
				else
					return true; // delete other link tag

			}
		}

		// 删掉p:标签,删掉o:标签
		if (tagName.startsWith("p:") || tagName.startsWith("o:")
			|| "script".equals(tagName))
			return true;

		return false; // keep all tags except above
	}

	@Override
	protected boolean isSupportedStyle(String styleName)
	{
		if ("visibility".equals(styleName.trim()))
			return false; // 把<div id=SlideObj 里面的visibility:hidden去掉, 保证可以显示
		return true; // keep all style, style is important to PPT
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
