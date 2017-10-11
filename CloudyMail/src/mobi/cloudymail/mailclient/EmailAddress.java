package mobi.cloudymail.mailclient;


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
//		if(addr.indexOf('@', posAT + 1) > 0)
//			return; //more than one @ is not allowed
		if (posLT > 0 && posGT > 0)
		{
			name = addr.substring(0, posLT).trim();
			address = addr.substring(posLT + 1, posGT).trim();
		}
		else
		{
//			name = address = addr.trim();
		    String[] reciverName = addr.split("@");
		    name=reciverName[0];
		    address=addr.trim();
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
		return address != null;
			
//		Pattern p = Pattern.compile("\\w+@\\w+\\;?"); //fail to match liu@126.com
//		Matcher m=p.matcher(address);
//		return m.matches();

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
		if(o == null || (! (o instanceof EmailAddress)) || !this.isValid())
			return false;
		EmailAddress a = (EmailAddress) o;
		return this.address.equals(a.getAddress());
	}
}