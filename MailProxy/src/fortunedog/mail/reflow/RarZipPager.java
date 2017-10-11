package fortunedog.mail.reflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fortunedog.mail.proxy.MailClient;
import fortunedog.mail.proxy.servlet.SetCharacterEncodingFilter;
import fortunedog.util.Utils;

public class RarZipPager extends ContentPager
{
	public static String winrarPath;
	static Logger log = LoggerFactory.getLogger(RarZipPager.class);
	static{
		
		Context env;
		try
		{
			env = (Context) new InitialContext().lookup("java:comp/env");
			winrarPath = (String) env.lookup("winrarPath");	
		}
		catch (NamingException e)
		{
			e.printStackTrace();
			winrarPath="winrar";
		}
	}
	class InternalHierFile
	{
		private String _filePath;
		private String _fileName;
		private String _sizeStr="";
		private boolean _dirFlag = false;//directory flag
		private ContentPager _pager = null;
		private int _idxInParent=-1;
		private InternalHierFile _parent = null;
		//used for embedded compressed file, after extracted, new hierarchical file list will be generated.
		private InternalHierFile _linkedFile = null;
		public Vector<InternalHierFile> _children=new Vector<InternalHierFile>();
		
		public InternalHierFile(String filePath,String fileName,InternalHierFile parent,
				int idx,boolean dirFlag)
		{
			_fileName = fileName;
			_filePath =  filePath;
			_parent = parent;
			_idxInParent = idx;
			_dirFlag = dirFlag;
		}
		
		public String getFileSizeStr()
		{
			if(_dirFlag)
				return _sizeStr;
			if(!_sizeStr.equals(""))
				return _sizeStr;
			File file = new File(_filePath);
			try
			{
				FileInputStream fis = new FileInputStream(file);
				_sizeStr = getHumanReadableSize(fis.available());
				fis.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				_sizeStr = "";
			}
			return _sizeStr;
		}
		
		public ContentPager getPager(HttpSession session)
		{
			if(_pager==null)
			{
				
				if(_dirFlag)//cd into folder.
				{
					HtmlPager htPager = new HtmlPager();
					StringBuffer strBuf= new StringBuffer();
					fillHeader(strBuf, Utils.getClientLocale(session).getLanguage());
					htPager.initWithoutFilter("<p>"+strBuf.toString()+"</p><table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">"+getChildrenList(Utils.getClientLocale(session).getLanguage())+"</table>");
					_pager = (ContentPager)htPager;
				}
				else
				{// preview
					try
					{
						_pager = PagerFactory.getPager(_fileName, session);
						if(_pager instanceof RarZipPager)
						{
							RarZipPager tmpPager = (RarZipPager)_pager;
							tmpPager.setUid(_uid);
							tmpPager.setFolderName(_folderName);
							tmpPager.setAttachIdx(_attachIdx);
							tmpPager._fileBeforeExtract = this;
						}
						_pager.init(new File(_filePath));
					}
					catch (Exception e)
					{
						log.warn("create zip pager:", e);
					}
					assert (_pager != null);
				}
			}
			return _pager;
		}
		
		private String getChildrenList(String clientLang)
		{
			if(_children.isEmpty())
				return null;
			StringBuffer strBuf = new StringBuffer();
			ResourceBundle rb = Utils.getResourceBundle(SetCharacterEncodingFilter.getCurrentRequest());
			for(int i = 0; i < _children.size(); i++)
			{
				strBuf.append("<tr bgColor=\"#cccccc\"><td>");
				InternalHierFile child = _children.get(i);
				if(child._dirFlag)
				{
					strBuf.append(child.getFileItem(clientLang)+"</td></tr>");
					continue;
				}
				else {
					strBuf.append(child._fileName);
				}
				String internalPath = child.getInternalPath();
				strBuf.append("</td><td>"+child.getFileSizeStr()+"</td><td>");
				
				strBuf.append("<button style=\"height:35;width:85\" ");
				strBuf.append("onClick=\"window.cmail.downloadAttachment");
				strBuf.append("("+getAttachIdx()+",'"+child._fileName+"','"+internalPath+"')\">"+rb.getString("download")+"</button>");
				if(PagerFactory.canPreview(child._fileName))
				{//add preview
					strBuf.append("<button style=\"height:35;width:85\" ");
					strBuf.append("onClick=\"window.cmail.openAttachment");
					strBuf.append("("+getAttachIdx()+",'"+child._fileName+"','"+internalPath+"')\">"+rb.getString("preview")+"</button>");
				}
				strBuf.append("</td></tr>");
			}
			return strBuf.toString();
		}
		
		public String getFileItem(String linkStr, String clientLang)
		{
			StringBuffer strBuf=new StringBuffer();
			strBuf.append("<a href=\"");
			strBuf.append(getSharedUrl(clientLang));
			String internalPath = getInternalPath();
			if(internalPath!=null && !internalPath.equals(""))
				strBuf.append("&internalPath="+Utils.encodeUrlParam(internalPath));
			strBuf.append("\">"+linkStr+"</a>");
			return strBuf.toString();
		}
		
		public String getFileItem(String clientLang)
		{
			return getFileItem(_fileName, clientLang);
		}
		
		//return the hierarchical path with index.
		public String getInternalPath()
		{
			if(_parent == null || _idxInParent < 0)
				return "";
			String parentPath = _parent.getInternalPath();
			if(parentPath==null || parentPath.equals(""))
				return ""+_idxInParent;
			else 
				return parentPath+"."+_idxInParent;
		}
		
		private String getSharedUrl(String clientLang)
		{
			
			return "/MailProxy2/ViewPart?uid=" + Utils.encodeUrlParam(getUid()) 
					+"&folderName="+ Utils.encodeUrlParam(getFolderName()) 
					+"&index=" + getAttachIdx()+"&pageNo=0"
					+"&lang="+clientLang;
			
		}
		
		//show the file path in rar package.
		public void fillHeader(StringBuffer strBuf, String clientLang)
		{
			if(_parent != null)
			{
				_parent.fillHeader(strBuf, clientLang);
				strBuf.append("/ ");
			}
			strBuf.append(getFileItem(clientLang));
		}
	}
	
	
	private File tempDownLoadFile;
	private InternalHierFile _rootFile=null;
	//this variable for embedded compressed file.
	//current compressed file's corresponding InternalFile before extracted, it should be passed to root file after extracted.
	private InternalHierFile _fileBeforeExtract=null;
	private String _uid=null;
	private String _folderName = null;
	private int _attachIdx=-1;
	//current internal file's path to preview.
	private String _curInternalPath=null;

	public void setUid(String uid)
	{
		_uid = uid;
	}
	public String getUid()
	{
		return _uid;
	}
	public void setAttachIdx(int attachIdx)
	{
		_attachIdx = attachIdx;
	}
	public int getAttachIdx()
	{
		return _attachIdx;
	}
	public String getCurInternalPath()
	{
		return _curInternalPath;
	}
	public void setCurInternalPath(String internalPath)
	{
		_curInternalPath = internalPath;
	}
	
	
	@Override
	public int getPageCount()
	{
		return getPager(getInternalFileIndexArray()).getPageCount();
	}

	private int[] getInternalFileIndexArray()
	{
		int idxArray[] = null;
		if(_curInternalPath==null || _curInternalPath.equals(""))
		{
			idxArray = new int[0];
		}
		else {
			String idxStrArray[] = _curInternalPath.split("\\.");
			idxArray = new int[idxStrArray.length];
			for(int i=0; i < idxArray.length; i++)
				idxArray[i] = Integer.valueOf(idxStrArray[i]).intValue();
		}
		return idxArray;
	}
	@Override
	public String renderPage(int pageNo)
	{
		return getPager(getInternalFileIndexArray()).renderPage(pageNo);
	}

	@Override
	public void init(InputStream is, String charset) throws IOException
	{
		try
		{
			tempDownLoadFile = File.createTempFile("mail", "." + getProperty(ContentPager.PROP_FULL_FILE_EXT, "rar") );
			tempDownLoadFile.deleteOnExit();
			FileOutputStream os = new FileOutputStream(tempDownLoadFile);
			byte[] buffer = new byte[1024*1024];
			int len;
			while( (len = is.read(buffer)) >= 0)
			{
				os.write(buffer, 0, len);
			}
			os.close();
			doExtract();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void doExtract()
	{
		//extract the zip/rar file with winrar
		String filePath = tempDownLoadFile.getAbsolutePath();
		String targetDir = filePath.substring(0, filePath.lastIndexOf("."))+"\\";
		String[] cmds = { winrarPath, "x", "-ibck", "-inul","-pnull","-y",
		                  filePath, targetDir};
		try
		{
			Process p = Runtime.getRuntime().exec(cmds);
			int result = p.waitFor();
			System.out.println("The exit code:"+result);
			if(result != 0)
				return;
			String orgFileName= getProperty(ContentPager.PROP_FILE_NAME, "");
			if(_fileBeforeExtract == null)
				_rootFile = new InternalHierFile(targetDir,orgFileName,null,-1,true);
			else //for embedded compressed file.
			{
				_rootFile = new InternalHierFile(targetDir,orgFileName,_fileBeforeExtract._parent,_fileBeforeExtract._idxInParent,true);
				_fileBeforeExtract._linkedFile = _rootFile;
			}
			//print extracted files.
			LinkedList<File> list = new LinkedList<File>();
			LinkedList<InternalHierFile> interDirList = new LinkedList<InternalHierFile>();
			File dir = new File(targetDir);
			File file[] = dir.listFiles();
	        for (int i = 0; i < file.length; i++) {
	        	boolean dirFlag = file[i].isDirectory();
	        	InternalHierFile tmpHFile = new InternalHierFile(file[i].getAbsolutePath(),
	        	                                                 file[i].getName(),
	        	                                                 _rootFile,i,dirFlag);
	        	
	        	_rootFile._children.add(tmpHFile);
	            if (dirFlag)
	            {
	                list.add(file[i]);
	                interDirList.add(tmpHFile);
	            }
	            else
	                System.out.println(file[i].getAbsolutePath());
	        }
	        File dirFile;
	        InternalHierFile dirHFile;
	        while (!list.isEmpty() && !interDirList.isEmpty()) {
	        	dirFile = (File)list.removeFirst();
				dirHFile = interDirList.removeFirst();
				file = dirFile.listFiles();
				if (file == null)
					continue;
				for (int i = 0; i < file.length; i++)
				{
					boolean dirFlag = file[i].isDirectory();
					InternalHierFile tmpHFile = new InternalHierFile(
													file[i].getAbsolutePath(),
													file[i].getName(),
													dirHFile,
													i,
													dirFlag);
					dirHFile._children.add(tmpHFile);
					if (dirFlag)
					{
						list.add(file[i]);
						interDirList.add(tmpHFile);
					}
					else
						System.out.println(file[i].getAbsolutePath());
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private InternalHierFile getInternalFile(int fileIdxs[])
	{
		InternalHierFile curFile = _rootFile;
		for(int i = 0; i < fileIdxs.length; i++)
		{	
			if(curFile._children.isEmpty() && curFile._linkedFile != null)
				curFile = curFile._linkedFile;
			assert(fileIdxs[i] < curFile._children.size());
			curFile = curFile._children.get(fileIdxs[i]);
		}
		return curFile;
	}
	public ContentPager getPager(int fileIdxs[])
	{
		return getInternalFile(fileIdxs).getPager(getSession());
	}

	protected void finalize() throws Throwable {  
		if(_rootFile!=null)
			org.apache.commons.io.FileUtils.deleteQuietly(new File(_rootFile._filePath));
		tempDownLoadFile.delete();
	    super.finalize();  
	}  
	
	public void visit(PageVisitor visitor)
	{
		visitor.visitPager(this);
	}
	
	public String getFilePath(String internalPath)
	{
		String idxStrArray[] = internalPath.split("\\.");
		int idxArray[] = new int[idxStrArray.length];
		for(int i=0; i < idxArray.length; i++)
			idxArray[i] = Integer.valueOf(idxStrArray[i]).intValue();
		return getInternalFile(idxArray)._filePath;
	}
	
	public void setNavBar(String pageNavBar)
	{
		getPager(getInternalFileIndexArray()).setNavBar(pageNavBar);	
	}
	/**
	 * @param _folderName
	 * 
	 */
	public void setFolderName(String folderName)
	{
		_folderName = folderName;
		
	}
	
	public String getFolderName()
	{
		return _folderName;
	}
}
