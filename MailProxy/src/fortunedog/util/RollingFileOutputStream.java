package fortunedog.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RollingFileOutputStream extends OutputStream
{

	private final static int ROLL_COUNT = 2;
	private final static int thresshold = 1000;
	private long writeCounter = 0;
	private final String stemName;
	private FileOutputStream out = null;
	private final File src;
	private final File target;
	public RollingFileOutputStream(String name) throws FileNotFoundException
	{
		
		stemName = name;
		src = new File(stemName + ".1");
		target = new File(stemName + ".2");
		writeCounter = src.length();
		out = new FileOutputStream(src);
		
	}
	@Override
	public void write(int b) throws IOException
	{
		out.write(b);
		writeCounter++;
		rollOver();
	}
	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		out.write(b, off, len);
		writeCounter += len;
		rollOver();
	}

	private void rollOver() throws IOException
	{
		if(writeCounter < thresshold)
			return;
		if(out != null)
			out.close();
		if(target.exists())
			target.delete();
		src.renameTo(target);
		out = new FileOutputStream(src);
	}
}
