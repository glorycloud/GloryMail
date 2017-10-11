package fortunedog.mail.proxy.net;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.servlet.SetCharacterEncodingFilter;

public class Result
{
	public static final int FAIL = 1;
	public static final int SUCCESSED = 0;
	public static final int AUTH_FAIL = 2;
	public static final int MSGSEND_FAIL = 3;
	public static final int NEEDLOGIN_FAIL = 4;
	static Logger log = LoggerFactory.getLogger(Result.class);
//	public static enum ResultStatus{
//		SUCCESSED (0), FAIL(1), AUTH_FAIL( 2), MSGSEND_FAIL(3), NEEDLOGIN_FAIL(4);
//		private final int id;
//		ResultStatus(int id) { this.id = id; }
//	    public int getValue() { return id; }
//	};
	public Result()
	{
		status = SUCCESSED;
	}
	
	public Result(int status, String reason)
	{
		this.status = status;
		this.failReason = reason;
	}
	
	public int getStatus()
	{
		return status;
	}
	
	public void setSuccessed()
	{
		this.status = SUCCESSED;
		failReason = null;
	}
	
	public void setFail(String failReason)
	{
		this.status = FAIL;
		this.failReason = failReason;
	}
	
	public boolean isSuccessed()
	{
		return status == SUCCESSED;
	}
	
	public String getFailReason()
	{
		return failReason;
	}


	public void serialize(java.io.Writer out)
	{
		Element root = new Element("result");
		root.setAttribute("class", this.getClass().getName());
		Element status = new Element("status");
		status.setAttribute("code", this.status + "");
		if(failReason != null)
			status.setAttribute("reason", failReason );
		root.addContent(status);
		
		Element content = new Element("content");
		fillContent(content);
		root.addContent(content);
		
		Document doc =new Document();
		XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());
		doc.setRootElement(root);
		try
		{
			output.output(doc, out);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch(NullPointerException t)
		{
			
			
			StringBuilder sb = new StringBuilder("==================BEGIN NULL POINT ENCOUNTED!!\n");
			if(out == null)
			{
				sb.append( "object out is null\n");
				
			}
			sb.append("Result has class:" + this.getClass().getName());
			if(this instanceof MailListResult)
			{
				sb.append(((MailListResult)this).dump(this));
			}
			StringWriter w = new StringWriter(3000);
			PrintWriter dout = new PrintWriter(w);
			
			dout.println("EXCEPTION :");
			t.printStackTrace(dout);
			SetCharacterEncodingFilter.dumpAssistData.get().dump(dout);
			dout.close();
			IOUtils.closeQuietly(w);
			sb.append(w.getBuffer());
			sb.append("=========================END==============");
			log.info( sb.toString());
			
		}
		
	}
	
	
	protected void fillContent(Element content)
	{
		
	}
	
	/**
	 * status of a net call. 0 for successful, other for failure
	 * for nonzero, status is the error code
	 */
	private int status;

	/**
	 * A string describe reason for failure. if status is scucessed, this is null;
	 */
	private String failReason;

}
