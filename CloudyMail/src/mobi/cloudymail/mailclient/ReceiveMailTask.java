package mobi.cloudymail.mailclient;

import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.mailclient.net.Account;
import mobi.cloudymail.mailclient.net.Result;
import mobi.cloudymail.mailclient.net.ServerAgent;
import mobi.cloudymail.util.MessageBox;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import mobi.cloudymail.util.Utils;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

class ReceiveMailTask extends AsyncTask<Boolean, Void, Result>
	{
		/**
		 * 
		 */
		public static interface ReceiveMailCallback
		{
			void receiveFinished(int status);
		}
		private HttpGet req = null;
		private int mailUidxCeil = -1;
		int mailCount = 0;
		private String errorMsg;
		InBoxActivity _mainWnd;
		private boolean receiveAll = false;
		private String folderName;
		boolean beQuiet= false;
		private ReceiveMailCallback onFinishCallBack;
		boolean scrollViewToTop = true;
		private String accountName=null;
		/**
		 * use this constructor to ask this task work on 'get more mail' mode.
		 * in this mode, task try to download mails whose uidx is not great than
		 * mailUidxCeil
		 * 
		 * @param mailUidxCeil
		 * @param inBoxActivity 
		 */
		public ReceiveMailTask(int mailUidxCeil,InBoxActivity mainWnd, boolean receiveAll, boolean quiet, boolean scrollViewToTop)
		{
			
			this( mailUidxCeil, mainWnd,  receiveAll,  quiet, scrollViewToTop, null);
		}
		public ReceiveMailTask(int mailUidxCeil,InBoxActivity mainWnd, boolean receiveAll, boolean quiet, boolean scrollViewToTop, ReceiveMailCallback onFinish)
		{
			this.mailUidxCeil = mailUidxCeil;
			this.receiveAll = receiveAll;
			_mainWnd = mainWnd;
			folderName = mainWnd.folderName;
			this.beQuiet = quiet;
			this.scrollViewToTop = scrollViewToTop;
			this.onFinishCallBack = onFinish;
			
		}

		
		public void abort()
		{
			this._mainWnd.setProgressBarVisible(false);
			cancel(true);
			if(req !=null)
				req.abort();
			((PullToRefreshListView)this._mainWnd.getExpandableListView()).onRefreshComplete(scrollViewToTop);
		}
		
//		@Override
//		public void onCancelled(Result o)
//		{
//			
//		}
		@Override
		protected Result doInBackground(Boolean... checkNewMail)
		{
			if(isCancelled())
				return null;
			Result rst = new Result();
			if(receiveAll)
			{
				for(int i=0;i<AccountManager.getCount();i++)
				 {
					if(isCancelled())
						return null;
					Account a = AccountManager.getByIndex(i);
					Result r = doReceiveOnce(a, checkNewMail);
					if(r==null)
						return null;
					else if(r.mailCount>0) 
						rst=r;
				 }
				
				//first time to start service
				MyApp.instance().startService(new Intent(MyApp.instance(), ReceiveMailService.class));

				return rst;
			}
			else
			{	
				accountName=MyApp.currentAccount.name;
				return doReceiveOnce(MyApp.currentAccount, checkNewMail);
			}
		
				
		}
       /**
        * insert mailInfo into group tables
        * 
        * tables column
        * [accoundId  Id  suffix]
        *  Id & groupId Ó³Éä
        * 
        * @param a
        * @param checkNewMail
        * @return
        */
		private Result doReceiveOnce(final Account a, Boolean... checkNewMail)
		{
			Log.d(Utils.LOGTAG, "Start doReceiveOnce");
			ServerAgent agent = MyApp.getAgent(a);
			int status=Result.FAIL;
			Result result = null;
			try
			{
				agent.setReceiving(true);
				if(Utils.isEmpty(agent.getSessionId(false, !beQuiet, !beQuiet)))
					return null;
				if(isCancelled())
					return null;
				boolean doLogin = false;
				_mainWnd.stoppedByUser = false;
				boolean checkNew = checkNewMail[0].booleanValue();
				if(mailUidxCeil != 0)
					checkNew = false;
				
receiveMailLoop:
				while (!_mainWnd.stoppedByUser)
				{
					if (doLogin)
					{
						if(!agent.interactiveLogin(false,!beQuiet, !beQuiet))
							return null;
						doLogin = false;
					}//-----------------------end if
					if(isCancelled())
						return null;
					String sql = "select max(uidx) from mail where accountId="+ a.id
									+ " and state in (" + MailStatus.MAIL_NEW
									+","+MailStatus.MAIL_READED
									+","+MailStatus.MAIL_LOCAL_DELETED
									+","+MailStatus.MAIL_DELETE_FOREVER
									+ ") and folder='"+folderName+"'";
					if (mailUidxCeil > 0)
					{
						sql += " and uidx < " + mailUidxCeil;
					}

					int normalMaxIdx = NewDbHelper.getInstance()
							.executScalar(sql, null);
					int deleteMaxIdx = NewDbHelper
							.getInstance()
							.executScalar(	"select max(uidx) from mail where accountId="
													//+ MyApp.currentAccount.id
													+ a.id
													+ " and state=" + MailStatus.MAIL_DELETED
													+ " and folder='"+folderName+"'",
											null);
					
					
					errorMsg = null;

					try
					{
						//ServerAgent agent = MyApp.getAgent();
						
						String sid = agent.getSessionId(false,!beQuiet,!beQuiet);
						if(sid == null)
							return null;
						if(isCancelled())
							return null;
						req = new HttpGet(
											ServerAgent.getUrlBase()
													+ "/SyncupMail?nm="
													+ normalMaxIdx
													+ "&foldername="
													+ java.net.URLEncoder.encode(folderName, "UTF-8")
													+ "&dm="
													+ deleteMaxIdx
													+ (checkNew ? "&cn=1" : "")
													+ ((mailUidxCeil > 0)	? ("&LE=" + mailUidxCeil)
																			: "")
													+ (MyApp.userSetting.countPerReception > 0	? ("&mc=" + MyApp.userSetting.countPerReception)
																									: "")
													+ "&sid=" + java.net.URLEncoder.encode(sid));
						Log.d(Utils.LOGTAG, "To run Http:");
						HttpResponse rsp = ServerAgent.execute(req);
						Header[] headers = rsp.getHeaders("Content-Type");
						if(headers == null || headers.length == 0 || !headers[0].getValue().startsWith("text/xml") )
						{
							return null;
						}
						//server doesn't set Content-Length in header
//						headers = rsp.getHeaders("Content-Length");
//						if(headers == null || headers.length == 0 || Integer.parseInt(headers[0].getValue()) <=0 )
//						{
//							return null;
//						}
						
//						/*
//						 * <Result>
//						 * 		<status code="1" reason="NEED_LOGIN"/>
//						 * 		<content>
//						 * 			<mail uid="aaa" subject="bbb" />
//						 * 		</content>
//						 * </Result>
//						 */
						///////////////////////////////////
						XmlPullParser parser=Xml.newPullParser();
						parser.setInput(rsp.getEntity().getContent(), "UTF-8");
						int eventType=parser.getEventType();
						String tagName=null;
						
						
						while(eventType!=XmlPullParser.END_DOCUMENT)
						{
							
							if(eventType==XmlPullParser.START_TAG)
							{
								tagName=parser.getName();
								if(tagName.equalsIgnoreCase("status"))
								{
									status=Integer.parseInt(parser.getAttributeValue(null, "code"));
									if (status == Result.NEEDLOGIN_FAIL)
									{
										doLogin = true;
										continue receiveMailLoop;
									}
									result = new Result();
									result.status = status;
								}
//									if(tagName.equalsIgnoreCase("content"))
//									{
//										hasMore =parser.getAttributeValue(null, "hasMore");
//										
//									}
								if (tagName.equalsIgnoreCase("mail"))
								{
								    
									if (status == Result.SUCCESSED)
									{
										int attachmentFlag=0;
										MailInfo mailInfo=new MailInfo();
										mailInfo.setUid(parser.getAttributeValue(0));
										mailInfo.setSubject(parser.getAttributeValue(1));
										String date=parser.getAttributeValue(2);
										if(Utils.isEmpty(date))
											break;
										mailInfo.setDate(Utils.netDateFormater.parse(date));
										mailInfo.setFrom(parser.getAttributeValue(3));
									    mailInfo.setUidx(Integer.parseInt(parser.getAttributeValue(5)));
									    mailInfo.setState(Integer.parseInt(parser.getAttributeValue(6)));
									    if((mailInfo.getState() & MailStatus.FLAG_HAS_MORE_PLACEHOLD)!=0)
									    {
									    	byte[] b = {(byte) 0xff};
									    	String s = new String(b);
									    	mailInfo.setSubject(s);
									    	mailInfo.setFrom(s);
									    }
									
									    mailInfo.setTo(parser.getAttributeValue(7));
									    mailInfo.setCc(parser.getAttributeValue(8));
									    
									    attachmentFlag=Integer.parseInt(parser.getAttributeValue(9));
									    mailInfo.setFolder(parser.getAttributeValue(10));
									 // mailInfo.setHasAttachment(attachmentFlag==0?false:true,false);
									    mailInfo.setAttachmentFlag(attachmentFlag);
									    mailInfo.setAccountId(a.id);
									    NewDbHelper.getInstance().insertMailsToDb(mailInfo,a.id);
									    if(mailInfo.getState()==MailStatus.MAIL_NEW)
									    	mailCount++;
										
									}
								}
							}
							//--------------------START_TAG
							try {
								eventType=parser.next();
							} catch (Exception e) {
								Log.d(Utils.LOGTAG, "",e);
							}
							
						}//end while
						Log.d(Utils.LOGTAG, "To updateMailGroupState");
						NewDbHelper.getInstance().updateMailGroupState();
						result.mailCount=mailCount;
						try
						{
							publishProgress(new Void[0]);
						}
						catch(Throwable t)
						{
							Log.d(Utils.LOGTAG, "",t);
						}
						Log.d(Utils.LOGTAG, "Finish doReceiveOnce");
						return result;//strange, this line is not executed, but the latest return at end of this function is executed
						
						
					}//--------------------end try req
					catch (XmlPullParserException e)
					{
						Log.d(Utils.LOGTAG, "",e);
					}
					catch (Exception ex)
					{
						errorMsg = ex.getMessage();
						Log.d(Utils.LOGTAG, "",ex);
						throw new Exception(errorMsg); // user stopped or other
														// error
					}
					finally
					{
						
					}
				}//--------------------end while(stopbyuser)
			}//--------------end try 
			catch (Exception ex)
			{// receive terminate abnormal, stopped by user or other condition
				if (!_mainWnd.stoppedByUser)
					errorMsg = ex.getMessage();
			}
			finally
			{
				agent.setReceiving(false);
				if (!_mainWnd.stoppedByUser)
				{
					if (req != null)
					{
						req.abort();
						req = null;
					}
				}
				try {
					if(onFinishCallBack!=null)
						onFinishCallBack.receiveFinished(status);
				}
				catch(Throwable t) 
				{
					Log.d(Utils.LOGTAG, "",t);
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(Result result)
		{
			Log.d(Utils.LOGTAG, "onPostexecute");
			String accountMsg="";
			if(!beQuiet)
			{
				if(!receiveAll)
					accountMsg=accountName+"\r\n";
				Resources res = MyApp.instance().getResources();
				if (errorMsg != null && result == null)
					MessageBox.show(_mainWnd,accountMsg +errorMsg,
									res.getString(R.string.error));
//				prgDialog.hide();
				if (result != null && result.mailCount < 1)
					MessageBox.show(_mainWnd,
					                accountMsg+res.getString(R.string.noNewMail),
									res.getString(R.string.receiveMail));
			}
			this._mainWnd.setProgressBarVisible(false);
			((PullToRefreshListView)this._mainWnd.getExpandableListView()).onRefreshComplete(scrollViewToTop);
			// MailClient.this.totalMailCount += mailCount;
			// MailClient.this.setTitle(PRODUCT_NAME + "(" + mailCount + ")");
			Log.d(Utils.LOGTAG, "onPostexecute complete");
		}

		@Override
		protected void onPreExecute()
		{
			if (!ServerAgent.hasNetworkConnection() && !beQuiet)
			{
				this.cancel(true);
				this._mainWnd.setProgressBarVisible(false);
			    ((PullToRefreshListView) this._mainWnd.getExpandableListView()).onRefreshComplete(true);
			    Resources res = MyApp.getCurrentActivity().getResources();
				MessageBox.show(MyApp.getCurrentActivity(), res.getString(R.string.err_notConnected),
								res.getString(R.string.error));
			}
			else
			{
				this._mainWnd.setProgressBarVisible(true);
			}
		
		}

		@Override
		protected void onProgressUpdate(Void... values)
		{
			try 
			{
				_mainWnd.updateMail();
			}
			catch (Exception e)
			{
				Log.d(Utils.LOGTAG, "",e);
			}
		}
	}