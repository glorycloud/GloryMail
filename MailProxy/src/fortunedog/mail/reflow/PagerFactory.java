package fortunedog.mail.reflow;

import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpSession;

import fortunedog.util.Utils;


public class PagerFactory
{
	public static ContentPager getPager(String fileName, HttpSession session)
	{
		int dotPos = fileName.lastIndexOf('.');
		if(dotPos == -1 )
			return null;
		String ext = fileName.substring(dotPos + 1);
		ext = ext.toLowerCase();
		String className = pagerMap.get(ext);
		if(Utils.isEmpty(className))
			return null;
		//some file such as test.tar.gz, the full ext should be stored.
		dotPos = fileName.indexOf('.');
		String fullExt = fileName.substring(dotPos + 1);
		fullExt = fullExt.toLowerCase();
		Class c;
		try
		{
			c = Class.forName(className);
			ContentPager p = (ContentPager) c.newInstance();
			p.setProperty(ContentPager.PROP_FILE_EXT, ext);
			p.setProperty(ContentPager.PROP_FULL_FILE_EXT, fullExt);
			p.setProperty(ContentPager.PROP_FILE_NAME, fileName);
			p.setSession(session);
			return p;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private static String getPagerClassName(String fileName)
	{
		int dotPos = fileName.lastIndexOf('.');
		if(dotPos == -1 )
			return null;
		String ext = fileName.substring(dotPos + 1);
		ext = ext.toLowerCase();
		String className = pagerMap.get(ext);
		return className;
	}
	
	public static boolean canPreview(String fileName)
	{
		String className = getPagerClassName(fileName);
		if(Utils.isEmpty(className))
			return false;
		return true;
	}
	
	private static Map<String, String> pagerMap = new TreeMap<String, String>();
	
	static
	{
		pagerMap.put("doc", "fortunedog.mail.reflow.DocPager");
		pagerMap.put("docx", "fortunedog.mail.reflow.DocPager");
		pagerMap.put("htm", "fortunedog.mail.reflow.HtmlPager");
		pagerMap.put("html", "fortunedog.mail.reflow.HtmlPager");
		pagerMap.put("xml", "fortunedog.mail.reflow.HtmlPager");
		pagerMap.put("pdf", "fortunedog.mail.reflow.PdfPager2");
		pagerMap.put("txt", "fortunedog.mail.reflow.PlainTextPager");
		pagerMap.put("c", "fortunedog.mail.reflow.PlainTextPager");
		pagerMap.put("cpp", "fortunedog.mail.reflow.PlainTextPager");
		pagerMap.put("h", "fortunedog.mail.reflow.PlainTextPager");
		pagerMap.put("java", "fortunedog.mail.reflow.PlainTextPager");
		pagerMap.put("php", "fortunedog.mail.reflow.PlainTextPager");
		pagerMap.put("ppt", "fortunedog.mail.reflow.PptPager");
		pagerMap.put("pptx", "fortunedog.mail.reflow.PptPager");
		pagerMap.put("xls", "fortunedog.mail.reflow.ExcelPager");
		pagerMap.put("xlsx", "fortunedog.mail.reflow.ExcelPager");
		pagerMap.put("jpg", "fortunedog.mail.reflow.ImagePager");
		pagerMap.put("jpeg", "fortunedog.mail.reflow.ImagePager");
		pagerMap.put("png", "fortunedog.mail.reflow.ImagePager");
		pagerMap.put("gif", "fortunedog.mail.reflow.ImagePager");
		pagerMap.put("bmp", "fortunedog.mail.reflow.ImagePager");
		pagerMap.put("rar", "fortunedog.mail.reflow.RarZipPager");
		pagerMap.put("zip", "fortunedog.mail.reflow.RarZipPager");
		pagerMap.put("jar", "fortunedog.mail.reflow.RarZipPager");
		pagerMap.put("gz", "fortunedog.mail.reflow.RarZipPager");
		pagerMap.put("tar", "fortunedog.mail.reflow.RarZipPager");
		pagerMap.put("7z", "fortunedog.mail.reflow.RarZipPager");
		pagerMap.put("iso", "fortunedog.mail.reflow.RarZipPager");
		pagerMap.put("bz2", "fortunedog.mail.reflow.RarZipPager");
		pagerMap.put("cpp", "fortunedog.mail.reflow.PlainTextPager");
	}
}
