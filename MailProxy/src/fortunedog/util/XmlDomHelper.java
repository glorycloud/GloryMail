package fortunedog.util;

import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;

public class XmlDomHelper
{

	Document doc = null;
	public XmlDomHelper(Document doc)
	{
		this.doc = doc;
	}
	
	public static int getChildAsInt(Element e, String name) throws NoElementException
	{
		Element child = e.getElement(null, name);
		if(child == null)
			throw new NoElementException(name); 
		String n = child.getText(0);
		return Integer.parseInt(n);
	}
	public static int getChildAsInt(Element e, String name, int defValue) throws NoElementException
	{
		Element child = e.getElement(null, name);
		if(child == null)
			return defValue; 
		String n = child.getText(0);
		return Integer.parseInt(n);
	}
	
	public static String getChildAsString(Element e, String name, String defaultValue) throws NoElementException
	{
		Element child = e.getElement(null, name);
		if(child == null)
			return defaultValue; 
		String n = child.getText();
		return n;
	}
	
	public static String getChildAsString(Element e, String name) throws NoElementException
	{
		Element child = e.getElement(null, name);
		if(child == null)
			throw new NoElementException(name); 
		String n = child.getText();
		return n;
	}

	public static Element searchElement(Element root, String... path)
	{
		for(int i=0;i<path.length && root != null;i++)
		{
			root = root.getElement(null, path[i]);
			
		}
		return root;
	}
}
