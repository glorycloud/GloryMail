package fortunedog.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XmlPullHelper
{
	XmlPullParser parser;
	String tag;
	public XmlPullHelper(XmlPullParser parser)
	{
		this.parser = parser;
	}
	
	static public boolean nextElement(XmlPullParser p, String name) throws XmlPullParserException, IOException
	{
		int evt;
		while((evt = p.next()) != XmlPullParser.END_DOCUMENT)
		{
			if(evt == XmlPullParser.START_TAG && name.equals(p.getName()))
			{
				return true;
			}
		}
		return false;
	}
	static public boolean nextElement(XmlPullParser p) throws XmlPullParserException, IOException
	{
		int evt;
		try
		{
			while((evt = p.next()) != XmlPullParser.END_DOCUMENT )
			{
				if(evt==XmlPullParser.START_TAG)
					return true;
				
			}
		}
		catch(XmlPullParserException e)
		{
			
		}
		return false;
	}
	public boolean nextElement(String name) throws XmlPullParserException, IOException
	{
		return nextElement(parser, name);
	}
	public boolean nextElement() throws XmlPullParserException, IOException
	{
		return nextElement(parser);
	}
	

	public int getNextElementAsInt(String tagName) throws NumberFormatException, XmlPullParserException, IOException, NoElementException
	{
		if(nextElement(parser, tagName))
		{
			int evt = parser.next();
			String t = parser.getText();
			if(!Utils.isEmpty(t))
				return Integer.parseInt(t); 
		}
		throw new NoElementException(tagName);
	}
	
	public String getNextElementAsString(String tagName) throws NumberFormatException, XmlPullParserException, IOException, NoElementException
	{
		if(nextElement(parser, tagName))
		{
			int evt = parser.next();
			return parser.getText();
			
		}
		throw new NoElementException(tagName);
	}
	
	public String getCurrentTextIfElementIs(String tag) throws XmlPullParserException, IOException
	{
		if(tag.equals(parser.getName()))
		{
			parser.next();
			String t = parser.getText();
			Utils.ASSERT(parser.next() == XmlPullParser.END_TAG);
			parser.next();
			return t;
		}
		return null;
	}
	
	public HashMap<String, String> parseSimpleElement() throws XmlPullParserException, IOException
	{
		HashMap<String, String> map = new HashMap<>();
		int evt;
		while(( evt = parser.next()) == XmlPullParser.START_TAG)
		{
			String tag = parser.getName();
			evt = parser.next();
			if(evt == XmlPullParser.TEXT)
			{
				String t = parser.getText();
				map.put(tag, t);
				evt = parser.next();
				if(evt != XmlPullParser.END_TAG)
				{
					break;
				}
			}
			else
				break;
		}
		return map;
	}
}
