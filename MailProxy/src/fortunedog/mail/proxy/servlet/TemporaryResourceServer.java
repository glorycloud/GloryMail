package fortunedog.mail.proxy.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class TemporaryResourceServer extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static Hashtable<String, File> resources = new Hashtable();

	static public void addResource(String key, File location)
	{
		resources.put(key, location);
	}

	protected HttpSession checkSession(HttpServletRequest request)
	{

		String sid = request.getParameter("sid");
		HttpSession session = null;
		if (sid != null)
			session = SessionListener.getSession(sid);

		if (session == null)
			session = request.getSession(false);

		return session;
	}

	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
		HttpSession session = checkSession(request);
		if(session == null)
			return; //do nothing, since no session
		String pathInfo = request.getPathInfo();
		String key = pathInfo.substring(1);
		int pos = key.indexOf('/');
		if(pos > 0)
			key = key.substring(0, pos);
		
		File f = (File) session.getAttribute(key);
		if(f == null)
			return;
		
		String realFile = FilenameUtils.concat(f.getAbsolutePath(), pathInfo.substring(pathInfo.indexOf('/', 1) + 1));
		String contentType = URLConnection.guessContentTypeFromName(realFile);
		f = new File(realFile);
		if(f.exists())
		{
			FileInputStream is = new FileInputStream(f);
			OutputStream os = response.getOutputStream();
			response.setContentType(contentType);
			response.setContentLength((int)f.length());
			IOUtils.copy(is, os);
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(os);
		}
		
	}

}
