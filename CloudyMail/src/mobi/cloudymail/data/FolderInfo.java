/**
 * 
 */
package mobi.cloudymail.data;


/**
 * @author llm
 *
 */
public class FolderInfo
{
	public int accountId;
	public String folderName;
	public String displayName;
	public FolderInfo(int accountId, String folderName, String displayName)
	{
		super();
		this.accountId = accountId;
		this.folderName = folderName;
		this.displayName = displayName;
	}
	
	
}
