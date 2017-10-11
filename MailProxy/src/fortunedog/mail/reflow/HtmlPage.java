package fortunedog.mail.reflow;

class HtmlPage
{
	protected StringBuilder sb = new StringBuilder();
	private int size = 0;
	public int pageNo = -1; //0 based page number
	public HtmlPage nextPage = null;
	
	public HtmlPage(int pageNo)
	{
		this.pageNo = pageNo;
	}
	
	public void addTag(String tag) 
	{
		sb.append(tag);
	}
	public void addText(String para)
	{
		sb.append(para);
		size += para.length();
	}
	public void addCharReference(String refEntity)
	{
		sb.append(refEntity);
		size ++;
	}
	public int getSize() 
	{
		return size;
	}
}