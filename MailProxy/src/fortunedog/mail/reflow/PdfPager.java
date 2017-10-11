package fortunedog.mail.reflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.pdf.PdfReader;

import fortunedog.mail.proxy.MailPart;

public class PdfPager extends ContentPager
{
	static Logger log = LoggerFactory.getLogger(PdfPager.class);
	private static String pdf2htmlPath = "D:\\Downloads\\mobile_apps\\pdf2html\\pdf2html_cmd\\pdf2html.exe";
	private static String solidConverterPath = "SolidConverterPDF.exe";
	private static final File workDir = new File("C:\\temp\\mails");
	private static boolean useSolidPdf = false;
	private File outputDir;

	static{
		if(!workDir.exists())
			workDir.mkdirs();
//		InputStream stream = ClassLoader.getSystemResourceAsStream("fortunedog/setting.prop");
//		Properties properties = new Properties(); 
//		try 
//		{ 
//			properties.load(stream); 
//			pdf2htmlPath = properties.getProperty("cloudymail.proxy.pdf2html");
//		} 
//		catch (IOException e) { } 
		Context env;
		try
		{
			env = (Context) new InitialContext().lookup("java:comp/env");
			pdf2htmlPath = (String) env.lookup("pdf2htmlPath");
			solidConverterPath = (String) env.lookup("solidConverterPath");
			useSolidPdf = ((Boolean)env.lookup("useSolidPdf")).booleanValue();
		}
		catch (NamingException e)
		{
			e.printStackTrace();
		}
	}
	public PdfPager()
	{
		Random random = new Random();
		do 
		{
			outputDir = new File(workDir.getAbsolutePath()+File.separatorChar+"d"+random.nextInt());
		}while(outputDir.exists());
		outputDir.mkdirs();
		outputDir.deleteOnExit();
	}
	
	@Override
	public int getPageCount()
	{
		return pageCount;
	}

	@Override
	public void rawInit(MailPart p,String charset) throws IOException,MessagingException
	{
		tempPdfFile = File.createTempFile("mail", ".pdf" );
		tempPdfFile.deleteOnExit();
		try
		{
			p.saveFile(tempPdfFile);
		}
		catch (SQLException e)
		{
			throw new MessagingException("Fail load data", e);
		}
		FileInputStream fin = new FileInputStream(tempPdfFile);
		com.itextpdf.text.pdf.PdfReader reader = new PdfReader(fin);
		pageCount = reader.getNumberOfPages();
		fin.close();
	}
	
	@Override
	public void init(InputStream is, String charset) throws IOException
	{

		
		
	}
	
	public void init(File file) throws IOException
	{
		tempPdfFile = file;
		FileInputStream fin = new FileInputStream(tempPdfFile);
		com.itextpdf.text.pdf.PdfReader reader = new PdfReader(fin);
		pageCount = reader.getNumberOfPages();
		fin.close();
	}

	@Override
	public String renderPage(int pageNo)
	{
		if(pageNo < 0 || pageNo >= pageCount)
			return null;
		
		try
		{
			// 1. veryPDF2HTML			
			//use command line  pdf2html -f 1 -l 1 -m -q -c -i bshmanual.pdf -o out.html
			//directory out must exist before execute this command
//			String[] cmds = {pdf2htmlPath, "-f", (pageNo + 1) +"", "-l", (pageNo+1)+"", "-m", "-q", "-c", "-i", 
//					tempPdfFile.getAbsolutePath(), "-o", outputDir.getName()};
//			Process p = Runtime.getRuntime().exec(cmds, null, workDir);
//			p.waitFor();
//			InputStreamReader r = new InputStreamReader(p.getErrorStream(), "UTF-8");
//			File htmlFile = new File(outputDir.getAbsolutePath()+"/index.htm");

			File htmlFile = null;
			String pdfFileName = tempPdfFile.getCanonicalPath();
			int pos = pdfFileName.lastIndexOf('.');
			String htmlDirName=pdfFileName.substring(0, pos);
			if(!useSolidPdf)
			{
				// 2. use very pdf2html command line version			
				String[] cmds = { pdf2htmlPath, "-f", (pageNo + 1) + "", "-l",
									(pageNo + 1) + "", "-onehtm",
									tempPdfFile.getAbsolutePath(), htmlDirName };
				Process p = Runtime.getRuntime().exec(cmds, null, workDir);
				p.waitFor();
				InputStreamReader r = new InputStreamReader(p.getErrorStream(),
															"UTF-8");
				htmlFile = new File(htmlDirName + java.io.File.separator
											+ "index.htm");
			}
			else
			{
				// 3. use SolidPdfConverter
				String htmlFileName = pdfFileName.substring(0, pos) + ".htm";
				htmlFile = new File(htmlFileName);
				if (htmlFile.exists())
				{
					if (!htmlFile.delete())
					{
						log.warn( "Fail to delete html file:" + htmlFileName);
						return null;
					}
				}

				File scriptFile = File.createTempFile("script", ".script");
				String scriptContent = "<</FileName ("
										+ pdfFileName
										+ ") >> FileOpen\r\n"
										+ "<</OutputFolder ("
										+ htmlFile.getParent()
										+ ") \n"
										+ "/Pages ("
										+ (pageNo + 1)
										+ ")\n"
										+ "/GraphicsAsImages true \n"
										+ "/Images /Embed \n"
										+ "/LaunchViewer false>> ConvertToHtml\r\n"
										+ "FileClose Exit\r\n";
				scriptContent = scriptContent.replace("\\", "\\\\");
			//	FileWriter fw = new FileWriter(scriptFile);
				OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(scriptFile), "UTF-8"); 
				fw.write(scriptContent);
				fw.close();
				String[] cmds = { solidConverterPath, "/i",
									scriptFile.getCanonicalPath(), "/f",
									"script" };
				Process p = Runtime.getRuntime().exec(cmds, null, workDir);
				p.waitFor();
				InputStreamReader r = new InputStreamReader(p.getErrorStream(),
															"UTF-8");
				scriptFile.delete();
			}

			if(htmlFile == null || !htmlFile.exists())
				return null;
			HtmlPager pager = new HtmlPager();
			pager.setNavBar(navBarHtmlString);
			pager.init(htmlFile, false);
			String s = pager.renderPage(0);
		
			if(!useSolidPdf)
				FileUtils.deleteQuietly(new File(htmlDirName));
			return s;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	protected void finalize() throws Throwable {  
		org.apache.commons.io.FileUtils.deleteQuietly(tempPdfFile);
		org.apache.commons.io.FileUtils.deleteQuietly(outputDir);
	    super.finalize();  
	}  

	private int pageCount = 0;
	private File tempPdfFile;
	
}
