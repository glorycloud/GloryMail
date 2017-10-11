package fortunedog.mail.reflow;

import static net.htmlparser.jericho.HTMLElementName.A;
import static net.htmlparser.jericho.HTMLElementName.AREA;
import static net.htmlparser.jericho.HTMLElementName.BASE;
import static net.htmlparser.jericho.HTMLElementName.BODY;
import static net.htmlparser.jericho.HTMLElementName.BUTTON;
import static net.htmlparser.jericho.HTMLElementName.COL;
import static net.htmlparser.jericho.HTMLElementName.COLGROUP;
import static net.htmlparser.jericho.HTMLElementName.DEL;
import static net.htmlparser.jericho.HTMLElementName.DIR;
import static net.htmlparser.jericho.HTMLElementName.FONT;
import static net.htmlparser.jericho.HTMLElementName.FORM;
import static net.htmlparser.jericho.HTMLElementName.IMG;
import static net.htmlparser.jericho.HTMLElementName.INPUT;
import static net.htmlparser.jericho.HTMLElementName.INS;
import static net.htmlparser.jericho.HTMLElementName.LINK;
import static net.htmlparser.jericho.HTMLElementName.MAP;
import static net.htmlparser.jericho.HTMLElementName.MENU;
import static net.htmlparser.jericho.HTMLElementName.META;
import static net.htmlparser.jericho.HTMLElementName.OBJECT;
import static net.htmlparser.jericho.HTMLElementName.OPTGROUP;
import static net.htmlparser.jericho.HTMLElementName.OPTION;
import static net.htmlparser.jericho.HTMLElementName.P;
import static net.htmlparser.jericho.HTMLElementName.PARAM;
import static net.htmlparser.jericho.HTMLElementName.SCRIPT;
import static net.htmlparser.jericho.HTMLElementName.SELECT;
import static net.htmlparser.jericho.HTMLElementName.STYLE;
import static net.htmlparser.jericho.HTMLElementName.TABLE;
import static net.htmlparser.jericho.HTMLElementName.TD;
import static net.htmlparser.jericho.HTMLElementName.TH;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StartTagType;
import net.htmlparser.jericho.StreamedSource;
import net.htmlparser.jericho.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.servlet.SetCharacterEncodingFilter;
import fortunedog.util.Utils;

/**
 * @测试 IMG标签里面的属性，src是否大小写敏感，程序里面是按小写的src访问的
 * @author Daniel
 *
 */
public class HtmlPager extends ContentPager
{

	static Logger log = LoggerFactory.getLogger(HtmlPager.class);
	/**
	 * normal state, content can append to page
	 */
	private final static int FILLING_CONTENT = 1; 

	/**
	 * page content is long enough, wait a appropriate input to end 
	 */
	private final static int WAITING_END = 2;
	

//	private static String[] forbidEndTags = new String[] {BASE, BR, COL, IMG, PARAM, 
//		AREA, LINK, HR, "input", "basefont", 
//		"frame", "img", "input", "isindex", "meta",
//		};

	private static String[][] tagAttributes = new String[][] { 
		{BASE, "href"}, 
		{P, "style"},
		{TABLE, "border", "width"},
		{TH, "rowspan", "colspan", "width", "height"},
		{TD, "rowspan", "colspan", "width", "height"},
		{A, "href"},
		{IMG, "src", "alt"},
		{FONT, "face", "size", "color"},
		{HTMLElementName.SPAN, "style"},
		};
	
	private static String[] deleteTags = new String[] { LINK, INS,
		DEL, DIR, MENU, COLGROUP, COL, 
		PARAM, MAP, AREA, 
		INPUT, BUTTON, SELECT, OPTGROUP, OPTION,
		META};
	private static String[] wholeDeleteTags = new String[] { SCRIPT, STYLE, OBJECT, FORM};
//	private static String[] trEndTags = new String[] { TR, TABLE, TBODY, TFOOT, THEAD, };
	private static String[] supportedStyle = new String[] {"font-size", "font-family","color"}; 

	private static Map<String, String[]> tagAttrMap = new TreeMap<String, String[]> ();
	private static Set<String> deleteTagSet = new TreeSet<String>();
	private static Set<String> wholeDeleteTagSet = new TreeSet<String>();
//	private static Set<String> trEndTagSet = new TreeSet<String>();
	private static Set<String> supportedStyleSet = new TreeSet<String>();
	
	///variable for page HTML
	Stack<StartTag> tagStack = new Stack<StartTag>();
	///end variable for page HTML
	
	private Vector<HtmlPage> pages = new Vector<HtmlPage>();

	//set this variable to true, so IMG will be filtered or replaced with a placeholder
	private boolean doImgFilter = true;

//	private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(HtmlPager.class.getName());

	static 
	{
		//Arrays.sort(forbidEndTags);
//		for(String tag : forbidEndTags)
//		{
//			forbidEndTagSet.add(tag);
//		}
		for(String tag : deleteTags)
		{
			deleteTagSet.add(tag);
		}
		for(String[] attr : tagAttributes)
		{
			tagAttrMap.put(attr[0], attr);
		}
		for(String tag : wholeDeleteTags)
		{
			wholeDeleteTagSet.add(tag);
		}
		for(String s : supportedStyle)
		{
			supportedStyleSet.add(s);
		}
		
//		log.setLevel(Level.WARNING);
//		java.util.logging.Handler h = new ConsoleHandler();
//		h.setLevel(Level.FINE);
//		log.addHandler(h);
	}
	
	public boolean isDoImgFilter() {
		return doImgFilter;
	}

	public void setDoImgFilter(boolean doImgFilter) {
		this.doImgFilter = doImgFilter;
	}



	public HtmlPager()
	{
	}
	

	
	public HtmlPager(boolean filterImg)
	{
		setDoImgFilter(filterImg);
	}
	
	public void init(InputStreamReader reader, boolean multiPage)
	{
		try
		{
			readAllPages(new StreamedSource(reader), multiPage);
		}
		catch (Exception e)
		{
			log.error( "HtmlPager init fail", e);
		}

	}
	public void init(String bodyStr, boolean multiPage)
	{
		try
		{
			readAllPages(new StreamedSource(bodyStr), multiPage);
		}
		catch (Exception e)
		{
			log.error( "HtmlPager init fail with bodyStr", e);
		}	
	}
	
	public void initWithoutFilter(String bodyStr)
	{
		HtmlPage page = new HtmlPage(0);
		page.sb.append(bodyStr);
		pages.add(page);
	}
	
	public void init(File file, boolean multiPage)
	{
		FileInputStream inputStream = null;
		try
		{
			inputStream = new FileInputStream(file);
			readAllPages(new StreamedSource(inputStream ),multiPage);
		}
		catch (Exception e)
		{
			log.error( "HtmlPager init fail with file", e);
		}
		finally
		{
			if(inputStream != null)
			{
				try
				{
					inputStream.close();
				}
				catch (IOException e)
				{
				}
			}
		}

	}
	public void init(InputStream is, String charset)
	{
		try
		{
			readAllPages(new StreamedSource(is), true);
		}
		catch (Exception e)
		{
			log.error( "HtmlPager init fail with InputStream", e);
		}
		
	}
	@Override
	public int getPageCount()
	{
		return pages.size();
	}

	@Override
	public String renderPage(int pageNo)
	{
		HtmlPage page = getPage(pageNo);
		if(page == null)
			return "";
		return page.sb.toString();
	}

	protected boolean isSupportedAttribute(String tagName, Attribute attribute)
	{
		return true;
//		String[] attrs = tagAttrMap.get(tagName);
//		if(attrs == null)
//			return false;
//		String attrName = attribute.getName();
//		for(int i=1; i< attrs.length; i++)
//		{
//			assert attrName.equals(attrName.toLowerCase());
//			if(attrName.equals(attrs[i]))
//				return true;
//		}
//		return false;
	}


	protected boolean isDeleteTag(String tagName, Tag tag)
	{
		return deleteTagSet.contains(tagName);
	}

	protected boolean isWholeDeleteTag(String tagName)
	{
		return wholeDeleteTagSet.contains(tagName);
	}

	private void completePage(HtmlPage page)
	{
		for(int i=tagStack.size() -1; i>= 0; --i)
		{
			beforeAddTag(page, tagStack.get(i));
			page.addTag("</"+tagStack.get(i).getName() + ">");
			afterAddTag(page, tagStack.get(i));
		}
	}
	
	private void startPage(HtmlPage page)
	{
		for(int i=0 ; i < tagStack.size(); ++i)
		{
			beforeAddTag(page, tagStack.get(i));
			page.addTag(tagStack.get(i).tidy());
			afterAddTag(page, tagStack.get(i));
		}
	}


	
	private void pushTag(StartTag tag)
	{
		if(tagStack.size() > 0 && !tagStack.peek().isEndTagRequired())
			tagStack.pop();
		if(!tag.isEndTagForbidden())
			tagStack.push(tag);
//		String tagName = tag.toUpperCase();
//		if(Arrays.binarySearch(forbidEndTags, tagName) < 0)
//		{
//			tagStack.push(tagName);
//		}
	}

	private void popTag(EndTag tag)
	{//startTag.getName()==HTMLElementName.SELECT
		
		for(int i=tagStack.size()-1; i >= 0; i--)
		{
			if(tagStack.get(i).getName().equals(tag.getName()))
			{
				tagStack.setSize(i);
				break;
			}
		}
	}

	private HtmlPage getPage(int pageIdx)
	{
		if(pageIdx >=0 && pageIdx < pages.size())
			return pages.get(pageIdx);
		else
			return null;
	}
	
	private void readAllPages(StreamedSource streamedSource, boolean useMultiPage)
	{
		try
		{
			int lastSegmentEnd = 0;
			int pageNo = 0;
			HtmlPage currentPage = new HtmlPage(pageNo++);
			int pageState = FILLING_CONTENT;
			Iterator<Segment> it = streamedSource.iterator();
			ResourceBundle rb = Utils.getResourceBundle(SetCharacterEncodingFilter.getCurrentRequest());
			while (it.hasNext())
			{
				Segment segment = it.next();
				if (segment.getEnd() <= lastSegmentEnd)
					continue; // if this tag is inside the previous tag
								// (e.g. a server tag) then ignore it as
								// it was already output along with the
								// previous tag.
				lastSegmentEnd = segment.getEnd();
				if (segment instanceof StartTag)
				{
					StartTag tag = (StartTag) segment;
					if(tag.getTagType() == StartTagType.COMMENT)
						continue;
					String tagName = tag.getName();
					if(tagName == META)
					{
						
					}
					if(isDeleteTag(tagName, tag))
						continue;
					if(isWholeDeleteTag(tagName))
					{
						while(it.hasNext())
						{
							Segment end = it.next();
							if(end instanceof EndTag && ((EndTag) end).getName() == tagName)
								break;
						}
						continue;
					}
					if(tag.getTagType() == StartTagType.DOCTYPE_DECLARATION)
						continue;
					
					// HANDLE TAG
					// Uncomment the following line to ensure each tag
					// is valid XML:
					// writer.write(tag.tidy()); continue;
					Map<String, String> newAttrs = new TreeMap<String,String>();
					if(doImgFilter  && tagName == IMG)
					{
						setFlag(FLAG_IMAGE);
						String src = tag.getAttributeValue("src");
						//maybe there's no attribute "src", fix bug 173.
						if(src != null && src.startsWith("cid:"))
						{
							currentPage.sb.append("<div class=\"img_box\"><p>"+ rb.getString("inlineImage")+"&nbsp")
								.append(tag.getAttributeValue("alt"))
								.append("</p><a href=\"#\" onclick=\"return showImage(this,'")
								.append(URLEncoder.encode(src.substring(4), "UTF-8"))
								.append("')\" ")
								
								.append(">"+rb.getString("viewImage")+"</a></div>");
							
						}
						tag = null;
					}
					else
					{
						Attributes attrs = tag.getAttributes();
						if(attrs != null)
						{
							Iterator<Attribute> iter = attrs.iterator();
							while(iter.hasNext())
							{
								Attribute attr = iter.next();  
								if(isSupportedAttribute(tagName, attr))
								{
									String attrName = attr.getName();
									if("style".equals(attrName))
									{
										String newStyle = onStyle(tag, attr);
										if(newStyle.length() != 0)
											newAttrs.put("style", newStyle.toString());
									}
									else
										newAttrs.put(attrName, attr.getValue());
									
								}
								
							}
						}
					}
					if(pageState == WAITING_END)
					{
						completePage(currentPage);
						pages.add(currentPage);
						currentPage = new HtmlPage(pageNo++);
						System.out.println("Page No:"+currentPage.pageNo);							
						startPage(currentPage);
						pageState = FILLING_CONTENT;
					}
					if(tag != null)
					{
						pushTag(tag);
						beforeAddTag(currentPage, tag);
						currentPage.addTag(StartTag.generateHTML(tagName, newAttrs, tag.isEmptyElementTag()));
						afterAddTag(currentPage, tag);
					}
				}
				else if(segment instanceof EndTag)
				{
					EndTag tag = (EndTag) segment;
					String tagName = tag.getName();
					if(isDeleteTag(tagName, tag))
						continue;
					popTag(tag);
					beforeAddTag(currentPage, tag);
					currentPage.addTag(tag.tidy());
					afterAddTag(currentPage, tag);
					
//page should end on encounter a start tag, but not an end tag						
//					if(pageState == WAITING_END)
//					{
//						completePage(currentPage);
//						pages.add(currentPage);
//						
//						currentPage = new HtmlPage();
//						System.out.println("\nPage No:"+pageNo++);							
//						startPage(currentPage);
//						pageState = FILLING_CONTENT;
//					}
				
				}
				else if (segment instanceof CharacterReference)
				{
					CharacterReference characterReference = (CharacterReference) segment;
					// HANDLE CHARACTER REFERENCE
					// Uncomment the following line to decode all
					// character references instead of copying them
					// verbatim:
					// characterReference.appendCharTo(writer);
					// continue;
					currentPage.addCharReference(characterReference.toString());
				}
				else
				{
					// HANDLE PLAIN TEXT
					currentPage.addText(segment.toString());
					if(useMultiPage && currentPage.getSize() > MIN_PAGE_SIZE)
					{
						pageState = WAITING_END;
					}
				}
			}
			completePage(currentPage);
			pages.add(currentPage);
			
		}
		catch (Throwable t)
		{
			log.error("Error:", t);
		}
		
	}

	protected String onStyle(StartTag tag, Attribute attr)
	{
		String allStyle = attr.getValue();
		if(allStyle == null || allStyle.equals(""))
			return "";
		StringBuilder newStyle = new StringBuilder();
		String[] individualStyles = allStyle.split(";");
		for(String indStyle : individualStyles)
		{
			int colonPos = indStyle.indexOf(':');
			if(colonPos < 0)
				continue;
			String styleName = indStyle.substring(0, colonPos);
			if(isSupportedStyle(styleName))
			{
				newStyle.append(indStyle).append(";");
			}
		}
		return newStyle.toString();
	}

	protected boolean isSupportedStyle(String styleName) 
	{
		return supportedStyleSet.contains(styleName.trim());
	}
	
	protected void afterAddTag(HtmlPage page, Tag tag){}
	protected void beforeAddTag(HtmlPage page, Tag tag) 
	{
		if(tag instanceof EndTag && tag.getName() == BODY)
		{
			page.addText(navBarHtmlString);
		}
	}
}
