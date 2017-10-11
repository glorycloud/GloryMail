package mobi.cloudymail.mailclient.net;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import mobi.cloudymail.util.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Result 
{
	public static final int FAIL = 1;
	public static final int SUCCESSED = 0;
	public static final int AUTH_FAIL = 2;
	public static final int MSGSEND_FAIL = 3;
	public static final int NEEDLOGIN_FAIL = 4;
	public boolean hasContent = false;
	public Element xmlReader;
	private InputStream httpStream;
	private String contentType;
	public int mailCount=0;
	public Result()
	{
		status = SUCCESSED;
	}
	
	public Result(java.io.InputStream stream)
	{
		initXmlFromStream(stream);
	}

	private void initXmlFromStream(java.io.InputStream stream)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
     
		DocumentBuilder builder;
		try
		{
			builder = factory.newDocumentBuilder();
			Document dom = builder.parse(stream);
			Element root = dom.getDocumentElement();
			/*
			 * <Result>
			 * 		<status code="1" reason="NEED_LOGIN"/>
			 * 		<content>
			 * 			<mail uid="aaa" subject="bbb" />
			 * 		</content>
			 * </Result>
			 */
			NodeList l = root.getElementsByTagName("status");
			Utils.ASSERT(l.getLength() == 1);
			Element status = (Element) l.item(0);
			this.status = Integer.parseInt(status.getAttribute("code"));
			failReason = status.getAttribute("reason");
			NodeList items = root.getElementsByTagName("content");
	
	        if(items.getLength() > 0)
	        	xmlReader = (Element) items.item(0);
			
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		}
		catch (SAXException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public Result(java.io.InputStream stream, boolean useRawStream)
	{
		if(useRawStream)
			this.httpStream= stream;
		else
			initXmlFromStream(stream);
	}
	
	public InputStream getInputStream()
	{
		return httpStream;
	}
	public boolean isSuccessed()
	{
		
			return status == 0;
		
	}
	
	
	/**
	 * status of a net call. 0 for successful, other for failure
	 * for nonzero, status is the error code
	 */
	public int status;

	/**
	 * A string describe reason for failure. if status is scucessed, this is null;
	 */
	public String failReason;
	public void setContentType(String contentType)
	{
		this.contentType = contentType;
		
	}
	public String getContentType()
	{
		return contentType;
		
	}
	
	public boolean isXMLConent()
	{
		return contentType.startsWith("text/xml");
	}
}