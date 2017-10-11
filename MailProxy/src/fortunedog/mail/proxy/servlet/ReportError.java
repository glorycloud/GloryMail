package fortunedog.mail.proxy.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportError extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	static Logger log = LoggerFactory.getLogger("root");
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
		ServletInputStream s = request.getInputStream();
		System.out.println(""+request.getContentLength());
		byte[] buf = new byte[request.getContentLength()];
		int len = s.read(buf, 0, request.getContentLength());
		System.out.println("Actual input length:"+len);
		String enc = "UTF-8";
		String str = new String(buf, enc);
		log.error( "ClientErrorReport:"+str);
	}

}
