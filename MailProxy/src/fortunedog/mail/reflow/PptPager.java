package fortunedog.mail.reflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.util.Utils;

public class PptPager extends ContentPager
{
	static Logger log = LoggerFactory.getLogger(PptPager.class);
	private int pageCount = 0;
	private File tempPptFile = null;
	HashMap<Integer, PptHtmlPager> pagers = new HashMap<Integer, PptHtmlPager>(13);
	private File firstPageFile;
	private static String pptConverterPath = "D:\\Downloads\\mobile_apps\\pptConverter.exe";
	static{
		try
		{
			Context env = (Context) new InitialContext().lookup("java:comp/env");
			pptConverterPath = (String) env.lookup("pptConverterPath");
		}
		catch (NamingException e)
		{
			log.error("Failed initialize ppt converter", e);
		}
	}

	@Override
	public void init(InputStream in, String charset)
	{
		try
		{
			tempPptFile = File.createTempFile("mail", "." + getProperty(ContentPager.PROP_FILE_EXT, "ppt") );
			tempPptFile.deleteOnExit();
			FileOutputStream os = new FileOutputStream(tempPptFile);
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
			log.error("Failed init with stream", e);
		}
		finally
		{
			
		}
	}

	@Override
	public int getPageCount()
	{
		return pageCount;
	}
	
	//@param pageNo 0 based page number, or -1 to get page count only
	private HtmlPager getPager(int pageNo) throws IOException
	{
		PptHtmlPager pager = pagers.get(pageNo);
		if(pager != null)
			return pager;
		String inFile = tempPptFile.getAbsolutePath(); // 要替换的ppt文件
		
		File tempHtmlFile = null;
		if(pageNo == 0 && firstPageFile != null)
			tempHtmlFile = firstPageFile;
		else
		{
			tempHtmlFile = File.createTempFile("mail", ".html");
			tempHtmlFile.deleteOnExit();
			Process p = Runtime.getRuntime().exec(new String[] {pptConverterPath, inFile, tempHtmlFile.getAbsolutePath(), (pageNo < 0 ? 0 : pageNo) + ""});
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
		
		
		

		String singleSlideFile = FilenameUtils.removeExtension(tempHtmlFile
				.getAbsolutePath())
									+ ".files"
									+ File.separatorChar
									+ "slide0001.html";
		HttpSession session = getSession();
		File resDir = new File(FilenameUtils.removeExtension(tempHtmlFile.getAbsolutePath())	+ ".files");
		String docBase = null;
		if(session != null)
		{
			String baseName = FilenameUtils.getBaseName(tempHtmlFile.getAbsolutePath());
			
			session.setAttribute(baseName, resDir);
			docBase=Utils.resourceBase+"/"+baseName;
		}

		pager = new PptHtmlPager(docBase);
		pager.setNavBar(navBarHtmlString);
		pager.init(new File(singleSlideFile), false);
		pagers.put(pageNo, pager);
		return pager;
	}
	/**
	 * pageNo is 0 based
	 */
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
			
			log.error("ailed to open attachment", e);
			return "Failed to open attachment";
		}
		String s = pager.renderPage(0);
		return s;
			


	}

	protected void finalize() throws Throwable
	{
		tempPptFile.delete();
		super.finalize();
	}
}

