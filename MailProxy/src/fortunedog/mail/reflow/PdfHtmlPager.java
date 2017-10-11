/**
 * 
 */
package fortunedog.mail.reflow;

import static net.htmlparser.jericho.HTMLElementName.A;
import static net.htmlparser.jericho.HTMLElementName.BASE;
import static net.htmlparser.jericho.HTMLElementName.FONT;
import static net.htmlparser.jericho.HTMLElementName.IMG;
import static net.htmlparser.jericho.HTMLElementName.P;
import static net.htmlparser.jericho.HTMLElementName.STYLE;
import static net.htmlparser.jericho.HTMLElementName.TABLE;
import static net.htmlparser.jericho.HTMLElementName.TD;
import static net.htmlparser.jericho.HTMLElementName.TH;

import java.util.Map;
import java.util.TreeMap;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.StartTag;

/**
 * ¹ýÂËµôscript,@font-face
 * @author llm
 *
 */
public class PdfHtmlPager extends HtmlPager
{
	private static String[][] tagAttributes = new String[][] { 
	                                                  		{BASE, "href"}, 
	                                                  		{P, "style"},
	                                                  		{TABLE, "border", "width"},
	                                                  		{TH, "rowspan", "colspan", "width", "height"},
	                                                  		{TD, "rowspan", "colspan", "width", "height"},
	                                                  		{A, "href"},
	                                                  		{IMG, "src", "alt"},
	                                                  		{FONT, "face", "size", "color"},
	                                                  		{HTMLElementName.SPAN, "class",},
	                                                  		{HTMLElementName.DIV, "style", "class","id","data-page-no"},
	                                                  		};
//	private static String[] wholeDeleteTags = new String[] { SCRIPT, OBJECT, FORM};
//	private static String[] deleteTags = new String[] { LINK, INS,
//	                                            		DEL, DIR, MENU, COLGROUP, COL, 
//	                                            		PARAM, MAP, AREA, 
//	                                            		INPUT, BUTTON, SELECT, OPTGROUP, OPTION,
//	                                            		META};
//	private static Set<String> deleteTagSet = new TreeSet<String>();
//	private static Set<String> wholeDeleteTagSet = new TreeSet<String>();
	private static Map<String, String[]> tagAttrMap = new TreeMap<String, String[]>();

	static
	{
//		for(String tag : deleteTags)
//		{
//			deleteTagSet.add(tag);
//		}
		for(String[] attr : tagAttributes)
		{
			tagAttrMap.put(attr[0], attr);
		}
//		for(String tag : wholeDeleteTags)
//		{
//			wholeDeleteTagSet.add(tag);
//		}
	}


	protected boolean isWholeDeleteTag(String tagName)
	{
		if(tagName == STYLE)
			return false;
		else {
			
			return super.isWholeDeleteTag(tagName);
		}
	}

	@Override
	protected String onStyle(StartTag tag, Attribute attr)
	{
		return attr.getValue();//enable transfer image binary
	}
	protected boolean isSupportedAttribute(String tagName, Attribute attribute)
	{
		String[] attrs = tagAttrMap.get(tagName);
		if(attrs == null)
			return false;
		String attrName = attribute.getName();
		for(int i=1; i< attrs.length; i++)
		{
			assert attrName.equals(attrName.toLowerCase());
			if(attrName.equals(attrs[i]))
				return true;
		}
		return false;
	}
	@Override
	protected boolean isSupportedStyle(String styleName)
	{
		return true; // keep all style
	}
}
