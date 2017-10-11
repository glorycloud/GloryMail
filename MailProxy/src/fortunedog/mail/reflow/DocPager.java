package fortunedog.mail.reflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.servlet.SyncupMail;



public class DocPager extends ContentPager
{
	static Logger log = LoggerFactory.getLogger(DocPager.class);
	private int pageCount = 0;
	private File tempDocFile = null;
	HashMap<Integer, HtmlPager> pagers = new HashMap<Integer, HtmlPager>(13);
	private File firstPageFile;
	private static String wordConverterPath = "D:\\Downloads\\mobile_apps\\WordConverter.exe";
	static{
		try
		{
			Context env = (Context) new InitialContext().lookup("java:comp/env");
			wordConverterPath = (String) env.lookup("wordConverterPath");
		}
		catch (NamingException e)
		{
			e.printStackTrace();
		}
	}
	

	@Override
	public void init(InputStream in, String charset)
	{
		try
		{
			tempDocFile = File.createTempFile("mail", "." + getProperty(ContentPager.PROP_FILE_EXT, "doc") );
			tempDocFile.deleteOnExit();
			FileOutputStream os = new FileOutputStream(tempDocFile);
			byte[] buffer = new byte[1024*1024];
			int len;
			while( (len = in.read(buffer)) >= 0)
			{
				os.write(buffer, 0, len);
			}
			os.close();

			
			getPager(-1);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			
		}
		
		
	}

	//@param pageNo 0 based page number
	private HtmlPager getPager(int pageNo) throws IOException
	{
		HtmlPager pager = pagers.get(pageNo);
		if(pager != null)
			return pager;
		String inFile = tempDocFile.getAbsolutePath(); // 要替换的ppt文件
		
		
		File tempHtmlFile;
		if(pageNo == 0 && firstPageFile != null)
			tempHtmlFile = firstPageFile;
		else
		{
			tempHtmlFile = File.createTempFile("mail", ".html");
			Process p = Runtime.getRuntime().exec(new String[] {wordConverterPath, inFile, tempHtmlFile.getAbsolutePath(), (pageNo < 0 ? 0 : pageNo) + ""});
			try
			{
				p.waitFor();
				pageCount = p.exitValue();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			if(pageNo < 0)
			{
				firstPageFile = tempHtmlFile;
			}
		}
		if(pageNo < 0)
			return null;
		
		pager = new HtmlPager();
		pager.setNavBar(navBarHtmlString);
		pager.init(tempHtmlFile, false);
		pagers.put(pageNo, pager);
		FileUtils.deleteQuietly(tempHtmlFile);
		return pager;
	}

	@Override
	public int getPageCount()
	{
		return pageCount;
	}

	@Override
	public String renderPage(int pageNo)
	{
		HtmlPager pager;
		try
		{
			pager = getPager(pageNo);
		}
		catch (IOException e)
		{
			
			log.error("Fail to open attachment", e);
			return "Failed to open attachment";
		}
		String s = pager.renderPage(0);
		return s;
			
	}
	protected void finalize() throws Throwable {  
		tempDocFile.delete();
	    super.finalize();  
	}  
}
