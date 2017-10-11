package fortunedog.mail.reflow;

public class PageVisitor
{
	private String _uid=null;
	private String _folderName;
	private int _attachIdx=-1;
	private String _internalPath = null;
	public PageVisitor(String uid,String folderName, int attachIdx,String internalPath)
	{
		_uid = uid;
		_folderName = folderName;
		_attachIdx = attachIdx;
		_internalPath = internalPath;
	}
	
	public void visitPager(ContentPager pager)
	{
		//ignore;
	}
	
	public void visitPager(RarZipPager pager)
	{
		pager.setUid(_uid);
		pager.setFolderName(_folderName);
		pager.setAttachIdx(_attachIdx);
		pager.setCurInternalPath(_internalPath);
	}
}
