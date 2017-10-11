package fortunedog.mail.reflow;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.swing.JFrame;

import fortunedog.mail.proxy.MailPart;

public class ImagePager extends ContentPager
{

	static private JFrame frame=new JFrame();
	@Override
	public int getPageCount()
	{
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public String renderPage(int pageNo)
	{
		// TODO Auto-generated method stub
		return "<img src=\"/MailProxy2/temp/"+tempImageFile.getName()+"\"/>";
	}

	@Override
	public void init(InputStream is, String charset) throws IOException
	{
		resizeImage(is);
	}
	
	private void resizeImage(InputStream stream)
	{
		//now resize.
		try
		{
			BufferedImage srcBufImage = ImageIO.read(stream);
			BufferedImage bufTarget = null;
			  
			double sx = (double) FIXED_WIDTH / srcBufImage.getWidth();
			double sy = (double) FIXED_HEIGHT/ srcBufImage.getHeight();
			
			double factor;
			if(sx > 1 || sy > 1)//if picture is small than height 400 or  width 200, we don't need to do zoom it.
				factor = 1.;
			else
				factor = sx<sy?sx:sy;//use the minimum value.
			int targetWidth = (int)(srcBufImage.getWidth()*factor);
			int targetHeight = (int)(srcBufImage.getHeight()*factor);

			int type = srcBufImage.getType();
			if (type == BufferedImage.TYPE_CUSTOM)
			{
				ColorModel cm = srcBufImage.getColorModel();
				WritableRaster raster = cm
						.createCompatibleWritableRaster(targetWidth, targetHeight);
				boolean alphaPremultiplied = cm.isAlphaPremultiplied();
				bufTarget = new BufferedImage(cm, raster, alphaPremultiplied,
												null);
			}
			else
				bufTarget = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_USHORT_565_RGB );

			Graphics2D g = bufTarget.createGraphics();
			g.setRenderingHint(	RenderingHints.KEY_RENDERING,
								RenderingHints.VALUE_RENDER_QUALITY);
			g.drawRenderedImage(srcBufImage,
								AffineTransform.getScaleInstance(factor,factor));
			g.dispose();
			String r = getSession().getServletContext().getRealPath("/");
			
			tempImageFile = File.createTempFile("mail", ".jpg",new File(r, "temp"));
			ImageIO.write(bufTarget, "JPG",tempImageFile );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	@Override
	public void rawInit(MailPart p,String charset) throws IOException,MessagingException
	{
		String fileName = p.getFileName();
		int dotPos = fileName.lastIndexOf('.');
		if(dotPos == -1 )
		{
			System.err.println("attachment's file nade does not has any suffix.");
			return;
		}
		String ext = fileName.substring(dotPos + 1);
		ext = ext.toLowerCase();
		
		File tempImageFile1 = File.createTempFile("mail", "."+ext );
		tempImageFile1.deleteOnExit();
		try
		{
			p.saveFile(tempImageFile1);
		}
		catch (SQLException e)
		{
			throw new MessagingException("Fail load data", e);
		}
		//now resize.
		resizeImage(new FileInputStream(tempImageFile1));

	}

	private File tempImageFile;
	private final int FIXED_WIDTH = 200;
	private final int FIXED_HEIGHT = 400;
}
