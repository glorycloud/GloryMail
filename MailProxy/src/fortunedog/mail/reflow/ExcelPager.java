package fortunedog.mail.reflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import fortunedog.util.Utils;

public class ExcelPager extends ContentPager
{

	private int pageCount = 0;
	private File tempHtmlFile = null;
	private static String excelConverterPath = "D:\\Downloads\\mobile_apps\\OfficeConverter.exe";
	static{
		try
		{
			Context env = (Context) new InitialContext().lookup("java:comp/env");
			excelConverterPath = (String) env.lookup("excelConverterPath");
		}
		catch (NamingException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void init(InputStream in, String charset)
	{
//		ActiveXComponent app = new ActiveXComponent("Excel.Application"); // 启动Excel
		try
		{
			File tempFile = null;
			tempFile = File.createTempFile("mail", "." + getProperty(ContentPager.PROP_FILE_EXT, "xls"));
			tempFile.deleteOnExit();
			FileOutputStream os = new FileOutputStream(tempFile);
			byte[] buffer = new byte[1024 * 1024];
			int len;
			while ((len = in.read(buffer)) >= 0)
			{
				os.write(buffer, 0, len);
			}
			os.close();
			String inFile = tempFile.getAbsolutePath(); // 要替换的ppt文件
			String outFile = FilenameUtils.removeExtension(inFile)+".html";
			
			

			tempHtmlFile = new File(outFile);
			tempHtmlFile.deleteOnExit();
			Process p = Runtime.getRuntime().exec(new String[] {excelConverterPath, inFile, outFile});
			try
			{
				p.waitFor();
				pageCount = p.exitValue();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			FileUtils.deleteQuietly(tempFile);
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
//			Dispatch.call(app, "Quit");

		}

	}

	@Override
	public int getPageCount()
	{
		return pageCount;
	}

	/**
	 * pageNo is 0 based
	 */
	@Override
	public String renderPage(int pageNo)
	{
		String singleSlideFile = FilenameUtils.removeExtension(tempHtmlFile.getAbsolutePath())
		                                   										+ ".files"
		                                   										+ File.separatorChar
		                                   										+ "sheet" + String.format("%03d", pageNo+1) +".html";
		HttpSession session = getSession();
		File resDir = new File(FilenameUtils.removeExtension(tempHtmlFile.getAbsolutePath()) + ".files");
		String docBase = null;
		if(session != null)
		{
			String baseName = FilenameUtils.getBaseName(tempHtmlFile.getAbsolutePath());
			
			session.setAttribute(baseName, resDir);
			docBase=Utils.resourceBase+"/"+baseName;
		}
 
		ExcelHtmlPager pager = new ExcelHtmlPager(docBase);
		pager.setNavBar(navBarHtmlString);
		File slideHtmlFile = new File(singleSlideFile);
		if(!slideHtmlFile.exists())//some excel file may have only one sheet
		{
			slideHtmlFile = tempHtmlFile;
			pageCount = 1;
		}
//		pager.init(new File(singleSlideFile), false);
		pager.init(slideHtmlFile, false);
		String s = pager.renderPage(0);

		
		try
		{
			FileUtils.forceDeleteOnExit(resDir);
		}
		catch (IOException e)
		{
			
		}
		return s;
	}

	protected void finalize() throws Throwable
	{
		tempHtmlFile.delete();
		super.finalize();
	}
}

