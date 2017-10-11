package fortunedog.mail.proxy.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedBack extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	static Logger log = LoggerFactory.getLogger("root");
	
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
		System.out.println("Enter feedback ");
		request.setCharacterEncoding("UTF-8");
		ServletInputStream s = request.getInputStream();
		System.out.println("" + request.getContentLength());
		byte[] buf = new byte[request.getContentLength()];
		int len = s.read(buf, 0, request.getContentLength());
		System.out.println("Actual input length:" + len);
		String enc = "UTF-8";
		String str = new String(buf, enc);
		writeTxtFile(str);
		log.error( "ClientErrorReport:" + str);
	}

	public static void writeTxtFile(String newStr) throws IOException {
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(new Date());
		String filein = date + newStr + "\r\n";

		java.io.FileWriter fw = new java.io.FileWriter("C:\\feedback.txt", true);
		java.io.PrintWriter pw = new java.io.PrintWriter(fw);
		pw.println(filein);
		pw.close();
		fw.close();
	}

}
