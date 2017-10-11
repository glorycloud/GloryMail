package mobi.cloudymail.mailclient;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.util.DialogResult;
import mobi.cloudymail.util.DialogUtils;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.SimpleCrypto;
import mobi.cloudymail.util.Utils;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class AccountManager 
{
	public final static String FirstAccount = "firstAccount";
	
	static interface AccountChangeListener
	{
		public void accountChanged();
	}
	
	private static final List<Account> accounts = new ArrayList<Account>();
	static {
		loadAccount(accounts);
	}
//	public ArrayList<AccountChangeListener> accountChangeListeners = new ArrayList<AccountChangeListener>(2);
	
	public static Account getAccount(String name)
	{
		for (Account a : accounts) 
		{
			if(a.name.equals(name))
				return a;
		}
		return null;
	}
    public static int getAccountIndex(Account a)
    {
    	int selectedIdx = accounts.indexOf(a);
		return selectedIdx;
    }
	
	
	public static Account getAccount(int accountId)
	{
		for (Account a : accounts) 
		{
			if(a.id == accountId)
				return a;
		}
		return null;
	}
	
	public static Account getByIndex(int index)
	{
		return accounts.get(index);
	}
	
	public static int getCount()
	{
		return accounts.size();
	}
    public AccountManager()
    {
    }    
 
    public static boolean isEmpty()
    {
    	return accounts.isEmpty();
    }
	private static void loadAccount(List<Account> accounts)
	{
		SQLiteDatabase db = NewDbHelper.getInstance().getReadableDatabase();
		Cursor cursor = db.rawQuery("select * from account", null);
		accounts.clear();
		if (cursor == null)
			return ;
		if (cursor.moveToFirst())
		{
			do
			{
				Account a = new Account();

				int cIdx = cursor.getColumnIndex("ID");
				a.id = cursor.getInt(cIdx);

				cIdx = cursor.getColumnIndex("name");
				a.name = cursor.getString(cIdx);

				cIdx = cursor.getColumnIndex("loginName");
				a.loginName = cursor.getString(cIdx);

				cIdx = cursor.getColumnIndex("mailPort");
				a.setMailPort(cursor.getInt(cIdx));

				cIdx = cursor.getColumnIndex("mailServer");
				a.mailServer = cursor.getString(cIdx);

				cIdx = cursor.getColumnIndex("serverType");
				a.serverType = cursor.getString(cIdx);

				cIdx = cursor.getColumnIndex("smtpPort");
				a.smtpPort = cursor.getInt(cIdx);

				cIdx = cursor.getColumnIndex("smtpServer");
				a.smtpServer = cursor.getString(cIdx);

				cIdx = cursor.getColumnIndex("useSSL");
				a.useSSL = (cursor.getInt(cIdx) == 0 ? false : true);

			
				cIdx = cursor.getColumnIndex("password");
				//decryption
				String decryptPassword=null;
				try
				{
					decryptPassword = cursor.getString(cIdx);
					if(!Utils.isEmpty(decryptPassword))
						decryptPassword = SimpleCrypto.decrypt("&cloudyServices@cloudymail.mobi&",decryptPassword);
				}
				catch (Exception e)
				{
					//e.printStackTrace();
				}
				a.password =decryptPassword;

				accounts.add(a);
			} while (cursor.moveToNext());
		}
		cursor.close();
		
	}
	public static Account deleteAccount(int index)
	{
		Account toDel = AccountManager.accounts.remove(index);
		MyApp.removeAgent(toDel);
		NewDbHelper.getInstance().deleteAccount(toDel);
		return toDel;	}
	
	public static void addAccount(Account a)
	{
		NewDbHelper.getInstance().addAccount(a);
		AccountManager.accounts.add(a);
		PushMailClient.getInstance().startPush(MyApp.instance());

		
	}
	public static void updateAccount(Account oldAccount, Account newAccount)
	{
		MyApp.removeAgent(oldAccount);
		NewDbHelper.getInstance().updateAccount(newAccount, oldAccount.name);
		AccountManager.accounts.set(accounts.indexOf(oldAccount), newAccount);
		MyApp.getAgent(newAccount).clearSessionId();  //检测到SessionId为空是重新登录，更新设置到服务器
		PushMailClient.getInstance().startPush(MyApp.instance());
		
	}
	
	public static boolean checkAccountAndCreate(Activity cureentUI)
	{
		if (MyApp.currentAccount == null)
		{
			if (DialogUtils
					.showModalMsgBox(	cureentUI,
					                 	MyApp.instance().getString(R.string.no_account_and_ask_to_create),
					                 	MyApp.instance().getString(R.string.whether_setup_account),
										EnumSet.of(	DialogUtils.ButtonFlags.Yes,
													DialogUtils.ButtonFlags.No)) == DialogResult.YES)
			{
				Intent intent = new Intent(MyApp.getCurrentActivity(), mobi.cloudymail.mailclient.FolderActivity.class);
				intent.putExtra(AccountManager.FirstAccount, true);

				cureentUI.startActivityForResult(intent, R.layout.account_manager);
			}
			
			return false;
		}
		return true;
	}
	
	public static List<Account> getAccounts()
	{
		return accounts;
	}
	
	public static List<Account> getAccountsFromDb()
	{
		List<Account> accounts = new ArrayList<Account>();
		loadAccount(accounts);
		return accounts;
	}
}

