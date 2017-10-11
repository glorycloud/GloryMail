package fortunedog.util;


public class EmailAddress
{
	private String name = null;
	private String address = null;
	public EmailAddress(String addr)
	{
		int posLT = addr.indexOf('<');
		int posGT = addr.indexOf('>');
		int posAT = addr.indexOf('@');
		if (posAT <= 0)
			return;
		if (posLT < 0 && posGT < 0)
		{
			name = address = addr.trim();
		}
		//189's imap will return such 'to' list: <xxxx@189.com> <NIL@NIL>
		if (posLT >= 0 && posGT > 0)
		{		
			address = addr.substring(posLT + 1, posGT).trim();
			if(posLT>0)
				name = addr.substring(0, posLT).trim();
			else
				name = address;
 		}
	}
	
	public EmailAddress(String name, String address)
	{
		this.name = name.trim();
		this.address = address.trim();
	}
	
	//eliminate the provided address from address list.
	public static String filterMailAddress(String addrs,String excludeAddr)
	{
		if(addrs.indexOf(excludeAddr) < 0)
			return addrs;
		String[] addrList = addrs.split(";");
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < addrList.length; i++)
		{
			EmailAddress ea = new EmailAddress(addrList[i]);
			if(ea.getAddress().equals(excludeAddr))
				continue;
			if(sb.length() > 0)
				sb.append(";");
			sb.append(ea);
		}
		return sb.toString();
	}
	
	public boolean isValid()
	{
		return (!address.equals("") && !name.equals(""));
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getAddress()
	{
		return address;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public void setAddress(String address)
	{
		this.address = address;
	}
	
	@Override
	public String toString()
	{
//		return String.format("<%s> %s", name, address);
		return String.format("%s <%s>", name, address);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o == null || (! (o instanceof EmailAddress)))
			return false;
		EmailAddress a = (EmailAddress) o;
		return this.address.equals(a.getAddress());
	}
}