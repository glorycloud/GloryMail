package fortunedog.mail.reflow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Vector;

import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.util.Utils;

/*
 * 测试要点：
 * 1. 一个小的段落后面跟一个大的段落，测试后面大段落的大小为不同值，MAX_PAGE_SIZE, 稍小于MAX_PAGE_SIZE, 稍大于MAX_PAGE_SIZE
 * 2. MAX_PAGE_SIZE与MIN_PAGE_SIZE之差为不同数值，比如小于MIN，等于MIN,大于MIN时对分页的影响。
 */
public class PlainTextPager extends ContentPager 
{
	static Logger log = LoggerFactory.getLogger(PlainTextPager.class);

	private boolean multiPage = false;

	public PlainTextPager() 
	{
		
	}

	public PlainTextPager(String content, boolean multiPage)
	{
		this.multiPage = multiPage;
		init(content);
	}
	public PlainTextPager(InputStream in, boolean multiPage) throws IOException
	{
		this.multiPage = multiPage;
		byte[] buf = new byte[in.available()];
		in.read(buf);
		UniversalDetector detector = new UniversalDetector(null);
		detector.handleData(buf, 0, buf.length);
		detector.dataEnd();
		String encoding = detector.getDetectedCharset();
		in = new ByteArrayInputStream(buf);
		init(in, encoding);
	}
	
	public PlainTextPager(String content)
	{
		init(content);
	}

	public void init(String content)
	{
		ArrayDeque<String>  paragraphs = new ArrayDeque<String>(Arrays.asList(content.split("\\r\\n|\\r|\\n")));
		if(!multiPage)
		{
			Page currentPage = new Page();
			while(!paragraphs.isEmpty()) 
			{
				String nextPara = paragraphs.poll();
				if(nextPara == null)
					continue;
				currentPage.addParagraph(nextPara);
			}
			pages.add(currentPage);
			pageCount = pages.size();
			return;
		}
		try 
		{

			
			while(!paragraphs.isEmpty()) 
			{
				String nextPara = paragraphs.poll();
				if(nextPara == null)
					break;
				Page currentPage = new Page();
				int nextParaSize = nextPara.length();
				
				
				while(currentPage.getSize() + nextParaSize < MAX_PAGE_SIZE  )
				{
					currentPage.addParagraph(nextPara);

					nextPara = paragraphs.poll();
					if(nextPara == null)
						break;
					nextParaSize = nextPara.length();
				}
				
				if(multiPage && currentPage.getSize() < MIN_PAGE_SIZE && nextPara != null)
				{//this happens when a tiny paragraph followed by a huge paragraph. to avoid 
				 //a tiny page being created, split the followed paragraph to more 
					
					String first = nextPara.substring(0, MAX_PAGE_SIZE - currentPage.getSize());
					String second = nextPara.substring(MAX_PAGE_SIZE - currentPage.getSize());
					paragraphs.addFirst(second);
					currentPage.addParagraph(first);
				}
				pages.add(currentPage);
			
			}
			
			pageCount = pages.size();
		} 
		catch (Exception e) 
		{
			log.error( "PlainTextPager init fail", e);
		}
	}

	public void init(InputStream is, String charset) throws IOException
	{
		byte[] buf;
		buf = new byte[is.available()];
		int bufSize = is.read(buf);
		if(charset == null && bufSize > 3)
		{  
	        charset = "gb18030";  
	        if (buf[0] == -1 && buf[1] == -2 )  
	        	charset = "UTF-16";  
	        if (buf[0] == -2 && buf[1] == -1 )  
	        	charset = "Unicode";  
	        if(buf[0]==-17 && buf[1]==-69 && buf[2] ==-65)  
	        	charset = "UTF-8";
		}
		
			
//		if(charset == null)
//		{
//			UniversalDetector detector = new UniversalDetector(null);
//			detector.handleData(buf, 0, buf.length);
//			detector.dataEnd();
//			charset = detector.getDetectedCharset();
//		}
		init((charset == null || charset.equals("")) ? new String(buf,0,bufSize) : new String(buf, 0,bufSize,charset));
	}
	@Override
	public int getPageCount() 
	{
		return pageCount;
	}

	@Override
	public String renderPage(int pageNo) 
	{
		if(pageNo < 0 || pageNo >= pageCount)
			return "";
		Page page = pages.get(pageNo);
		StringBuilder sb = new StringBuilder();
		for(String s : page.allParas)
		{
			sb.append("<p>");
			sb.append(s);
			sb.append("</p>");
		}
		sb.append(navBarHtmlString);
		return sb.toString();
	}

	private int pageCount = 0;
	
	private Vector<Page> pages = new java.util.Vector<Page>();
}

class Page
{
	
	public void addParagraph(String para)
	{
		size += para.length();
		allParas.add("<p>" + Utils.escapeStr(para) +"</p>");
	}
	
	public int getSize() 
	{
		return size;
	}
	protected Vector<String> allParas = new Vector<String>();
	private int size = 0;
}
