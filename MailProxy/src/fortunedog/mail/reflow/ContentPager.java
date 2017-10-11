package fortunedog.mail.reflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpSession;

import fortunedog.mail.proxy.MailPart;


public abstract class ContentPager
{
	/**
	 * maximum count of characters in one page
	 */
	static public int MAX_PAGE_SIZE = 1500;
	
	/**
	 * minimum count of characters in one page
	 */
	static public int MIN_PAGE_SIZE = 1000;

	static public final int FLAG_IMAGE = 1; 
	
	static public final String PROP_FILE_EXT = "fileext";
	static public final String PROP_FULL_FILE_EXT = "fullfileext";
	static public final String PROP_FILE_NAME = "filename";
	
	//a locker used by all pagers to operate on clipboard.
	public static Object clipboardLock = new Object();
	
	private int  flags = 0;
	
	private Properties prop = new Properties();
	private HttpSession session = null;
	
	static private Map<String, String> fileTypeImags=new HashMap<String, String>();
	
	protected String navBarHtmlString = ""; //上下翻页用得html，嵌入在网页的底部
	static
	{
		Context env;
		try
		{
			env = (Context) new InitialContext().lookup("java:comp/env");
			MIN_PAGE_SIZE = (Integer) env.lookup("minimumPageSize");
			MAX_PAGE_SIZE = (Integer) env.lookup("maximumPageSize");
		}
		catch (NamingException e)
		{
			
		}

	}
	/**
	 * get total page number of this message
	 * @return total page number
	 */
	public abstract int getPageCount();
	
	/**
	 * render a specified page in HTML. 
	 * Implementation of this function should append generated HMTL code to the StringBuilder
	 * passed in. 
	 * returned string is then rounded with a <body> </body> tag by caller.  
	 * @param pageNo specify which page to render
	 */
	public abstract String renderPage(int pageNo);
	
	public boolean hasImage()
	{
		return (flags & FLAG_IMAGE) != 0;
	}
	
	protected void setFlag(int flag)
	{
		flags |= flag;
	}

	public abstract void init(InputStream is, String charset) throws IOException;
	public void init(File file) throws IOException
	{
		FileInputStream fiStream = new FileInputStream(file);
		init(fiStream,null);
		fiStream.close();
	}
	public void rawInit(MailPart p,String charset) throws IOException,MessagingException
	{
		try
		{
			init(p.getInputStream(),charset);
		}
		catch (SQLException e)
		{
			throw new MessagingException("Fail load data", e);
		}
	}
	
	public Object setProperty(String key, String value)
	{
		return prop.setProperty(key, value);
	}
	
	public String getProperty(String key, String defaultValue) 
	{
		return prop.getProperty(key, defaultValue);
	}
	
	public HttpSession getSession()
	{
		return session;
	}

	public void setSession(HttpSession session)
	{
		this.session = session;
	}

	public void setNavBar(String pageNavBar)
	{
		navBarHtmlString = pageNavBar;
		
	}
	
	public void visit(PageVisitor visitor)
	{
		visitor.visitPager(this);
	}
	
	static public String getFileImage(String fileType)
	{
		fileType = fileType.toLowerCase();
		if(fileTypeImags.isEmpty())
		{
			fileTypeImags.put("pdf", "pdf.png");
			fileTypeImags.put("doc", "doc.png");
			fileTypeImags.put("docx", "docx.png");
			fileTypeImags.put("xls", "xls.png");
			fileTypeImags.put("xlsx", "xlsx.png");
			fileTypeImags.put("ppt", "ppt.png");
			fileTypeImags.put("pptx", "pptx.png");
			fileTypeImags.put("jpg", "img.png");
			fileTypeImags.put("png", "img.png");
			fileTypeImags.put("bmp", "img.png");
			fileTypeImags.put("gif", "img.png");
			fileTypeImags.put("psd", "img.png");
			fileTypeImags.put("tif", "img.png");
			fileTypeImags.put("svg", "img.png");
			fileTypeImags.put("rar", "rar.png");
			fileTypeImags.put("zip", "rar.png");
			fileTypeImags.put("txt", "txt.png");
			fileTypeImags.put("java", "txt.png");
			fileTypeImags.put("c", "txt.png");
			fileTypeImags.put("h", "txt.png");
			fileTypeImags.put("cpp", "txt.png");
			fileTypeImags.put("mp3", "mp3.png");
			fileTypeImags.put("aac", "mp3.png");
			fileTypeImags.put("wma", "mp3.png");
			fileTypeImags.put("3gp", "mp3.png");
			fileTypeImags.put("amr", "mp3.png");
			fileTypeImags.put("wav", "mp3.png");
			fileTypeImags.put("html", "html.png");
			fileTypeImags.put("htm", "html.png");
		}
		if(fileTypeImags.containsKey(fileType))
			return fileTypeImags.get(fileType);
		else {
			return "file.png";
		}
	}
	
	/**
	 * return the human readable size
	 * @param bytes byte number
	 * @return
	 */
	static public String getHumanReadableSize(int bytes)
	{
		String fileSizeString = "";
		DecimalFormat df = new DecimalFormat("#.00");
		if (bytes < (1 << 10))
			fileSizeString = df.format((double) bytes) + "B";
		else if (bytes < (1 << 20))
			fileSizeString = df.format((double) bytes / (1 << 10)) + "KB";
		else if (bytes < (1 << 30))
			fileSizeString = df.format((double) bytes / (1 << 20)) + "MB";
		else
			fileSizeString = df.format((double) bytes / (1 << 30)) + "GB";
		return fileSizeString;
	}
}
