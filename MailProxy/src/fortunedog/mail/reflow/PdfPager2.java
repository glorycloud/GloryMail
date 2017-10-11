package fortunedog.mail.reflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.io.FileUtils;

import fortunedog.mail.proxy.MailPart;

public class PdfPager2 extends ContentPager
{
//	C:\\cygwin\\home\\llm\\cygwinpdf2htmlEX\\pdf2htmlEX\\pdf2htmlEX.exe
	private static String pdf2htmlEXPath = "";
	private static final File workDir = new File("C:\\temp\\mails");
	private File outputDir;
	static
	{
		if (!workDir.exists())
			workDir.mkdirs();
		Context env;
		try
		{
			env = (Context) new InitialContext().lookup("java:comp/env");
			pdf2htmlEXPath = (String) env.lookup("pdf2htmlEXPath");
			
		}
		catch (NamingException e)
		{
			e.printStackTrace();
		}
	}

	public PdfPager2()
	{
		Random random = new Random();
		do
		{
			outputDir = new File(workDir.getAbsolutePath() + File.separatorChar + "d"
									+ random.nextInt());
		} while (outputDir.exists());
		outputDir.mkdirs();
		outputDir.deleteOnExit();
	}

	@Override
	public int getPageCount()
	{
		return pageCount;
	}
	
	@Override
	public void rawInit(MailPart p, String charset) throws IOException, MessagingException
	{
		tempPdfFile = File.createTempFile("mail", ".pdf");
		tempPdfFile.deleteOnExit();
		try
		{
			p.saveFile(tempPdfFile);
		}
		catch (SQLException e)
		{
			throw new MessagingException("Fail to save file", e);
		}
		getPageCount(tempPdfFile);
	}

	public void init(File file) throws IOException
	{
		tempPdfFile = file;
		getPageCount(tempPdfFile);
	}

	private void getPageCount(File file) throws IOException
	{
		String[] cmd = new String[] { "c:\\xpdf\\pdfinfo.exe", "-meta",
										tempPdfFile.getAbsolutePath() };
		Process process = Runtime.getRuntime().exec(cmd);
		InputStream in = process.getInputStream();
		InputStreamReader inputStreamReader = new InputStreamReader(in);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		String line;
		while ((line = bufferedReader.readLine()) != null)
		{
			if (line.startsWith("Pages:"))
			{
				int pos = line.indexOf(':');
				String count = line.substring(pos + 1);
				String[] num = count.split(" ");

				pageCount = Integer.parseInt(num[num.length - 1]);
				break;
			}

		}
	}

	@Override
	public String renderPage(int pageNo)
	{
		if (pageNo < 0 || pageNo >= pageCount)
			return null;

		try
		{
			File htmlFile = null;
			String pdfFileName = tempPdfFile.getCanonicalPath();
			int pos = pdfFileName.lastIndexOf('.');
			String htmlDirName = pdfFileName.substring(0, pos);
			String[] cmds = { pdf2htmlEXPath, "--dest-dir", htmlDirName,"--enable-font-face","0","-f", (pageNo + 1) + "",
								"-l", (pageNo + 1) + "", pdfFileName,
								java.io.File.separator + "index.htm" };
			Process p = Runtime.getRuntime().exec(cmds);
//			try
//			{
//				InputStream in=p.getErrorStream();
//		    	InputStreamReader inputStreamReader = new InputStreamReader(in);
//		    	BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//		        String text;
//		        while ((text=bufferedReader.readLine()) != null)
//				     System.out.println(text); 
//			}
//			catch (IOException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			p.waitFor();
			String htmlFileName=htmlDirName + java.io.File.separator + "index.htm";
			htmlFile = new File(htmlFileName);
			if (htmlFile == null || !htmlFile.exists())
				return null;
			PdfHtmlPager pager = new PdfHtmlPager();
			pager.setNavBar(navBarHtmlString);
			pager.init(htmlFile, false);
			String s = pager.renderPage(0);

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

	protected void finalize() throws Throwable
	{
		org.apache.commons.io.FileUtils.deleteQuietly(tempPdfFile);
		org.apache.commons.io.FileUtils.deleteQuietly(outputDir);
		super.finalize();
	}

	private int pageCount = 0;
	private File tempPdfFile;

	@Override
	public void init(InputStream is, String charset) throws IOException
	{

	}

}
