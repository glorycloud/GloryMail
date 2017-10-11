package fortunedog.mail.proxy.servlet;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetKeyFiles extends HttpServlet{

	private static final long serialVersionUID = 4280469286194550012L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException 
	{
		String accountName = request.getParameter("accountName");
		String psw = request.getParameter("password");
		assert(!accountName.equals("") && !psw.equals(""));
	
		String[] files = {"D:/public.key", "D:/private.key"};
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();        
		ZipOutputStream zos = new ZipOutputStream(baos);
		byte bytes[] = new byte[2048];
		
		response.setContentType("application/x-zip-compressed");
		ServletOutputStream sos = response.getOutputStream();
		
		for (String fileName : files) 
		{            
			FileInputStream fis = new FileInputStream(fileName); 
			BufferedInputStream bis = new BufferedInputStream(fis);             
			zos.putNextEntry(new ZipEntry(fileName));             
			int bytesRead;            
			while ((bytesRead = bis.read(bytes)) != -1) 
			{
				zos.write(bytes, 0, bytesRead);            
			}            
			zos.closeEntry();            
			bis.close();            
			fis.close();        
		}        
		
		zos.flush();        
		baos.flush();        
		zos.close();        
		baos.close();
		
		sos.write(bytes);
		sos.flush();
	}
}
