package fortunedog.mail.reflow;

import static net.htmlparser.jericho.HTMLElementName.IMG;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.StreamedSource;
import net.htmlparser.jericho.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.util.Utils;

/**
 * @测试 IMG标签里面的属性，src是否大小写敏感，程序里面是按小写的src访问的
 * @author Daniel
 *
 */
public class FullFormatHtmlPager extends ContentPager
{

	static Logger log = LoggerFactory.getLogger(FullFormatHtmlPager.class);

	//private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(FullFormatHtmlPager.class.getName());
	private HtmlPage resultPage;
	private String msgUid = "";
	private String folderName;
//	static 
//	{
//		log.setLevel(Level.WARNING);
//		java.util.logging.Handler h = new ConsoleHandler();
//		h.setLevel(Level.FINE);
//		log.addHandler(h);
//	}

	public FullFormatHtmlPager()
	{
	}
	
	public void init(String msgUid, String folderName, InputStreamReader reader, boolean multiPage)
	{
		this.msgUid = msgUid;
		this.folderName = folderName;
		try
		{
			readAllPages(new StreamedSource(reader), multiPage);
		}
		catch (Exception e)
		{
			log.error( "init fail, msgUid="+msgUid, e);
			
		}

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
			e.printStackTrace();
			log.error( "init fail with File, msgUid="+msgUid,e);
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
			readAllPages(new StreamedSource(is), false);
		}
		catch (Exception e)
		{
			log.error( "init fail with InputStream, msgUid="+msgUid,e);
		}
		
	}
	
	@Override
	public int getPageCount()
	{
		return 1;
	}

	@Override
	public String renderPage(int pageNo)
	{
		HtmlPage page = getPage(pageNo);
		
		if(page == null || pageNo != 0)
			return "";
		return page.sb.toString();
	}

	protected boolean isSupportedAttribute(String tagName, Attribute attribute)
	{
		
		return true;
	}


	protected boolean isDeleteTag(String tagName, Tag tag)
	{
		return false;
	}

	protected boolean isWholeDeleteTag(String tagName)
	{
		return false;
	}

	private HtmlPage getPage(int pageIdx)
	{
		if(pageIdx == 0)
			return resultPage;
		else
			return null;
	}
	
	private void readAllPages(StreamedSource streamedSource, boolean useMultiPage)
	{
		try
		{
			int pageNo = 0;
			resultPage = new HtmlPage(pageNo++);
			
			Iterator<Segment> it = streamedSource.iterator();
			while (it.hasNext())
			{
				Segment segment = it.next();
				
				if (segment instanceof StartTag)
				{
					StartTag tag = (StartTag) segment;
					String tagName = tag.getName();
					
					
					// HANDLE TAG
					// Uncomment the following line to ensure each tag
					// is valid XML:
					// writer.write(tag.tidy()); continue;
					if(tagName == IMG)
					{
						setFlag(FLAG_IMAGE);
						if(!Utils.isEmpty(tag.getAttributeValue("src")))
						{
							Map<String, String> newAttrs = new TreeMap<String,String>();
							Attributes attrs = tag.getAttributes();
							for(int i=0;i<attrs.getCount();i++)
							{
								Attribute attr = attrs.get(i);
								if("src".equalsIgnoreCase(attr.getName()))
								{
									String src = attr.getValue();
									if(src.startsWith("cid:"))
									{
										String newSrc="GetMailPart?uid="+URLEncoder.encode(msgUid, "UTF-8")+"&cid=" + URLEncoder.encode(src.substring(4))+"&folderName="+URLEncoder.encode(folderName); 
										newAttrs.put(attr.getName(), newSrc);
									}
									else
										newAttrs.put(attr.getName(), attr.getValue());
									
								}
								else
									newAttrs.put(attr.getName(), attr.getValue());
							}
							resultPage.addTag(StartTag.generateHTML(tagName, newAttrs, tag.isEmptyElementTag()));
						}
						else
						{
							resultPage.addTag(tag.tidy());
						}
					}
					else
					{
				//		System.err.println("mendynew:     "+tag.tidy());
				//		System.err.println("mendynew:     "+tag.toString());
						resultPage.addTag(tag.toString());
					}
				}
				else if(segment instanceof EndTag)
				{
					EndTag tag = (EndTag) segment;
					resultPage.addTag(tag.toString());
					
				}
				else
				{
					resultPage.addText(segment.toString());
				}
			}
		}
		catch (Throwable t)
		{
			log.error("FullFormatHtmlPager error", t);
		}
		
	}
}



