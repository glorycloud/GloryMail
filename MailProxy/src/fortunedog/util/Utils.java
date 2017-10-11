package fortunedog.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.servlet.SessionListener;

public class Utils {
	
	private static SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat netDateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static Logger log = LoggerFactory.getLogger(Utils.class);


	/**
	 * Convert a date to string.
	 * @param value The date value
	 * @param d The default value if the date is null
	 */
	public static String getDateDisplay(Date value, String d) {
		if (value == null) {
			return d;
		} else {
			return dateFmt.format(value);
		}
	}


	/**
	 * Format a decimal value.
	 * @param value The decimal value
	 * @param fraction The digital count of fraction part
	 */
	public static String formatDecimal(BigDecimal value, int fraction) {
		if(value == null)
		{
			return "";
		}
		String str = value.toString();

		// Split the digital string into two parts.

		String leftPart, rightPart;
		StringBuffer buf = new StringBuffer();
		int dotPos = str.indexOf('.');
		if (dotPos >= 0) {
			leftPart = str.substring(0, dotPos);
			rightPart = str.substring(dotPos + 1);
		} else {
			leftPart = str;
			rightPart = "";
		}

		// Process the part before ".".

		int len = leftPart.length();
		buf.append(leftPart);
		int secPos = len - 3;
		while (secPos > 0) {
			buf.insert(secPos, ',');
			secPos -= 3;
		}

		// Process the fraction part.

		buf.append('.');
		len = rightPart.length();
		if (len > fraction) {
			buf.append(rightPart.substring(0, fraction));
		} else {
			buf.append(rightPart);
			while (len < fraction) {
				buf.append('0');
				len++;
			}
		}

		return buf.toString();
	}

	// Escape a string in JavaScript string syntax.
	public static String escapeStr(String str) {
		if (str == null) {
			return "";
		}
		int len = str.length();
		StringBuilder buf = new StringBuilder(len + 10);
		for (int i = 0; i < len; i++) {
			char ch = str.charAt(i);
//			if (ch == '\\') {
//				buf.append("\\\\");
//			} else if (ch == '\'') {
//				buf.append("\\'");
//			} else if (ch == '\n') {
//				buf.append("\\n");
//			} else if (ch == '\r') {
//				buf.append("\\r");
//			} else if (ch == '\t') {
//				buf.append("\\t");
//			} else {
//				buf.append(ch);
//			}
			switch (ch)
			{
			case '&':
				buf.append("&amp;");
				break;
			case '<':
				buf.append("&lt;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			case '\'':
				buf.append("&apos;");
				break;
			case '\"':
				buf.append("&quot;");
				break;
			default:
				buf.append(ch);
			}
		}
		return buf.toString();
	}


 
    /*
     * if input str is null,return a empty string.this prevent the string "null"
     * presents on the screen
     */
    public static String formatString(String str)
    {
    	return (str==null)?"":str;
    }

	public static boolean isEmpty(String str)
	{
		return str == null || str.length() == 0;
	}
    
	public static void ASSERT(boolean b)
	{
		assert b;
	}
	
	public static byte[] readPostData(javax.servlet.http.HttpServletRequest request) throws IOException
	{
		ServletInputStream is = request.getInputStream();
		int len = request.getContentLength();
		if(len < 0)
			len = 1024;
		byte[] buf = new byte[len];
		int totalReaded = 0;
		int readLen = 0;
		while(readLen != -1 && totalReaded < len)
		{
			totalReaded += readLen;
			readLen = is.read(buf, totalReaded, len - totalReaded);
		}
		if(totalReaded == buf.length)
			return buf;
		return Arrays.copyOf(buf, totalReaded);
	}
	
//  use org.apache.commons.io.FileUtils.deleteQuietly(File f) instead
//	public static boolean rmdirRecursively(String path)
//	{
//		File file = new File(path);
//		String[] children = file.list();
//		for(String child : children)
//		{
//			if(!rmdirRecursively(child)) 
//			{
//				return false;
//			}
//		}
//		return file.delete();
//	}
	
	public static String ensureStringValidate(String str)
	{
		return str.replace("\0", "");
	}
	
	public static String encodeUrlParam(String paramValue)
	{
		try
		{
			return java.net.URLEncoder.encode(paramValue, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			
			e.printStackTrace();
			return paramValue;
		}
	}
	
	public static int getClientVersionCode(HttpSession session)
	{
		try 
		{
			return (Integer) session.getAttribute("clientVersion");
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		return 1;
	}

	public static ResourceBundle getResourceBundle(javax.servlet.http.HttpServletRequest req)
	{
		ResourceBundle bundle = ResourceBundle.getBundle( "strings", getClientLocale(req));
		return bundle;
	}
	


	public static Locale getClientLocale(javax.servlet.http.HttpServletRequest req)
	{
		if(req == null)
			return Locale.ENGLISH;
		String lang = req.getParameter("lang");
		if(Utils.isEmpty(lang))
		{
			HttpSession session = getSession(req, true);
			
			Locale l = getClientLocale(session);
			return l;
		}
		if(lang != null && lang.startsWith("en"))
			return Locale.ENGLISH;
		else
			return Locale.CHINESE;
		
	}


	public static Locale getClientLocale(HttpSession session)
	{
		Locale l = null;
		if(session != null)
		{
			l = (Locale)session.getAttribute("lang");
		}
		if(l == null)
			l=Locale.CHINESE;
		return l;
	}
	

	public static String urlBase="http://glorycloud.com.cn:8088/MailProxy2";
	public static String resourceBase = "Resource";


	public static HttpSession getSession(HttpServletRequest req, boolean useSid)
	{
		if(useSid)
		{
			String sid = req.getParameter("sid");
			if(sid != null)
				return SessionListener.getSession(sid);
			else
				return req.getSession(false);
		}
		else
			return req.getSession(false);
	}
}
