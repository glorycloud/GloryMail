package mobi.cloudymail.util;

import java.io.FileWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import android.os.Environment;
import android.os.Looper;
import android.util.Log;


public class Utils {
	public static final String LOGTAG = "CloudyMail";
	public static void logException(Exception ex) 
	{
		Log.d(Utils.LOGTAG, "Exception:",ex);
	}
	
	public static SimpleDateFormat dateFmtNoYear = new SimpleDateFormat("d MMM");
	public static SimpleDateFormat dateFmt = new SimpleDateFormat("d MMM yyyy");
	public static SimpleDateFormat earlierFormat = new SimpleDateFormat("yy/M/d H:mm");
	public static SimpleDateFormat nearFormat = new SimpleDateFormat("d MMM H:mm");
	public static SimpleDateFormat netDateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static SimpleDateFormat accurateDateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

	static
	{
		Locale l = Locale.getDefault();
		if(l.equals(Locale.CHINA) || l.equals(Locale.CHINESE))
		{
			dateFmtNoYear = new SimpleDateFormat("M月d日");
			dateFmt = new SimpleDateFormat("yyyy年M月d日");
			earlierFormat = new SimpleDateFormat("yy年M月d日 H:mm");
			nearFormat = new SimpleDateFormat("M月d日 H:mm");
		}
		else
		{
			dateFmtNoYear = new SimpleDateFormat("d MMM");
			dateFmt = new SimpleDateFormat("d MMM yyyy");
			earlierFormat = new SimpleDateFormat("d MMM yy H:mm");
			nearFormat = new SimpleDateFormat("d MMM H:mm");
		}
			
	}
	
	public static boolean isInChinese()
	{
		Locale l = Locale.getDefault();
		return l.equals(Locale.CHINA) || l.equals(Locale.CHINESE);
	}
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
	//
	// NOTE: Can't used to escape URL string. To escape URL, please use java.net.URLEncoder.encode(url)
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
	
//	public static byte[] readPostData(javax.servlet.http.HttpServletRequest request) throws IOException
//	{
//		ServletInputStream is = request.getInputStream();
//		int len = request.getContentLength();
//		if(len < 0)
//			len = 1024;
//		byte[] buf = new byte[len];
//		int totalReaded = 0;
//		int readLen = 0;
//		while(readLen != -1 && totalReaded < len)
//		{
//			totalReaded += readLen;
//			readLen = is.read(buf, totalReaded, len - totalReaded);
//		}
//		if(totalReaded == buf.length)
//			return buf;
//		return Arrays.copyOf(buf, totalReaded);
//	}
	
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
	
	public static boolean inUiThread()
	{
		return Looper.myLooper() != null;
	}
	
	public static void runOnUiThreadAndBlock(final Runnable r)
	{
		final Semaphore waitingSem = new Semaphore(0);
		Runnable r2 = new Runnable() {
			
			@Override
			public void run()
			{
				r.run();
				waitingSem.release();
			}
		};
		MyApp.getCurrentActivity().runOnUiThread(r2);
		try
		{
			waitingSem.acquire();
//			System.out.println("GOT it");
			return;
		}
		catch (InterruptedException e)
		{
			
			e.printStackTrace();
		}
	}
	static public String getReadableSize(long value)
	{
		double sizeF = value;
		String unit = "B";
		if (value > (1 << 30))
		{
			sizeF = sizeF / (1 << 30);
			unit = "GB";
		}
		else if (sizeF > (1 << 20))
		{
			sizeF = sizeF / (1 << 20);
			unit = "MB";
		}
		else if (sizeF > 1024)
		{
			sizeF = sizeF / (1024.0);
			unit = "KB";
		}
		NumberFormat nFormat = NumberFormat.getInstance();
		nFormat.setMaximumFractionDigits(1);
		return (nFormat.format(sizeF) + unit);
	}
	static public void log(String str)
	{
		Log.d(Utils.LOGTAG, str);
		if(!MyApp.isDebug)
			return;
		Date now = new Date();
		try
		{
			FileWriter fo = new FileWriter(Environment.getExternalStorageDirectory() + "/cloudmail.log.txt",true);
			
			fo.write(accurateDateFormater.format(now)+str+"\n");
			fo.close();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getWeekOfDate(Date date)
	{
//		String[] weekDaysName = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
		String[] weekDaysName = { "Sun.", "Mon.", "Tues.", "Wed.", "Thurs.", "Fri.", "Sat." };
		String[] weekDaysCode = { "0", "1", "2", "3", "4", "5", "6" };
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int intWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
		return weekDaysName[intWeek];
	}
}
