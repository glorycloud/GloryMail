/* Copyright 2010 Vivek Iyer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fortunedog.mail.proxy.exchange;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;

import jcifs.util.Hexdump;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.wap.WbxmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import fortunedog.mail.proxy.exchange.codepage.AirNotifyCodePage;
import fortunedog.mail.proxy.exchange.codepage.AirSyncBaseCodePage;
import fortunedog.mail.proxy.exchange.codepage.AirSyncCodePage;
import fortunedog.mail.proxy.exchange.codepage.CalendarCodePage;
import fortunedog.mail.proxy.exchange.codepage.CodePage;
import fortunedog.mail.proxy.exchange.codepage.ComposeMailCodePage;
import fortunedog.mail.proxy.exchange.codepage.Contacts2CodePage;
import fortunedog.mail.proxy.exchange.codepage.ContactsCodePage;
import fortunedog.mail.proxy.exchange.codepage.DocumentLibraryCodePage;
import fortunedog.mail.proxy.exchange.codepage.Email2CodePage;
import fortunedog.mail.proxy.exchange.codepage.EmailCodePage;
import fortunedog.mail.proxy.exchange.codepage.FolderHierarchyCodePage;
import fortunedog.mail.proxy.exchange.codepage.GALCodePage;
import fortunedog.mail.proxy.exchange.codepage.ItemEstimateCodePage;
import fortunedog.mail.proxy.exchange.codepage.ItemOperationsCodePage;
import fortunedog.mail.proxy.exchange.codepage.MeetingResponseCodePage;
import fortunedog.mail.proxy.exchange.codepage.MoveCodePage;
import fortunedog.mail.proxy.exchange.codepage.NotesCodePage;
import fortunedog.mail.proxy.exchange.codepage.PingCodePage;
import fortunedog.mail.proxy.exchange.codepage.ProvisionCodePage;
import fortunedog.mail.proxy.exchange.codepage.ResolveRecipientsCodePage;
import fortunedog.mail.proxy.exchange.codepage.RightsManagementCodePage;
import fortunedog.mail.proxy.exchange.codepage.SearchCodePage;
import fortunedog.mail.proxy.exchange.codepage.SettingsCodePage;
import fortunedog.mail.proxy.exchange.codepage.TasksCodePage;
import fortunedog.mail.proxy.exchange.codepage.ValidateCertCodePage;
import fortunedog.util.NoElementException;
import fortunedog.util.Utils;
import fortunedog.util.XmlDomHelper;

/**
 * @author Vivek Iyer
 *
 * This class is responsible for implementing the ActiveSync commands that
 * are used to connect to the Exchange server and  query the GAL
 */
/**
 * @author vivek
 * 
 */
public class ActiveSyncManager
{
	static Logger log = LoggerFactory.getLogger(ActiveSyncManager.class);
	private String mPolicyKey = "0"; //The policy key is sent to the server for all protocol command requests except for the Autodiscover command ([MS-ASCMD] section 2.2.2.1), the Ping command ([MS-ASCMD] section 2.2.2.11), and the HTTP OPTIONS command 
	private String mAuthString;
	private String mUri;
	private String mServerName;
	private String mDomain;
	private WBXML wbxml;
	private String mUsername;
	private String mPassword;
	private boolean mUseSSL;
	private boolean mAcceptAllCerts;
	private String mActiveSyncVersion = "";
	private int mDeviceId = 0;
	private float mActiveSyncVersionFloat = 0.0F;
	private String mEmail;
	private HttpClient httpclient = null;
	private boolean debug = true;
	private HashSet<String> supportedCommands = new HashSet<>();
	// private static final String TAG = "ActiveSyncManager";

	public static final int EXCH_STATUS_OK = 1;
	public static final int EXCH_STATUS_REPROVISION = 2;//When the server returns a status code<2> from any command indicating that the client needs to re-provision.
	static
	{
		
//		java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
//		java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
		//System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
//		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "false");
//		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "warning");
//		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "warning");
//		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "warning");
		
//		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Log4JLogger");
			CodePage[] pageList = new CodePage[25];
	    	
	    	pageList[0] = new AirSyncCodePage();
	    	pageList[1] = new ContactsCodePage();
	    	pageList[2] = new EmailCodePage();
	    	pageList[3] = new AirNotifyCodePage();
	    	pageList[4] = new CalendarCodePage();
	    	pageList[5] = new MoveCodePage();
	    	pageList[6] = new ItemEstimateCodePage();
	    	pageList[7] = new FolderHierarchyCodePage();
	    	pageList[8] = new MeetingResponseCodePage();
	    	pageList[9] = new TasksCodePage();
	    	pageList[10] = new ResolveRecipientsCodePage();
	    	pageList[11] = new ValidateCertCodePage();
	    	pageList[12] = new Contacts2CodePage();
	    	pageList[13] = new PingCodePage();
	    	pageList[14] = new ProvisionCodePage();
	    	pageList[15] = new SearchCodePage();
	    	pageList[16] = new GALCodePage();
	    	pageList[17] = new AirSyncBaseCodePage();
	    	pageList[18] = new SettingsCodePage();
	    	pageList[19] = new DocumentLibraryCodePage();
	    	pageList[20] = new ItemOperationsCodePage();
	    	pageList[21] = new ComposeMailCodePage();
	    	pageList[22] = new Email2CodePage();
	    	pageList[23] = new NotesCodePage();
	    	pageList[24] = new RightsManagementCodePage();
	    	
	    	for(int i=0;i<pageList.length;i++)
	    	{
	    		WbxmlParser.setTagTable(i, pageList[i].toArray());
	    	}
		
	}
	private float getActiveSyncVersionFloat()
	{
		if (mActiveSyncVersionFloat == 0.0F)
			mActiveSyncVersionFloat = Float.parseFloat(mActiveSyncVersion);
		return mActiveSyncVersionFloat;
	}

	public boolean isUseSSLSet()
	{
		return mUseSSL;
	}

	public void setUseSSL(boolean mUseSSL)
	{
		this.mUseSSL = mUseSSL;
	}

	public boolean isAcceptAllCertsSet()
	{
		return mAcceptAllCerts;
	}

	public void setAcceptAllCerts(boolean mAcceptAllCerts)
	{
		this.mAcceptAllCerts = mAcceptAllCerts;
	}

	public String getActiveSyncVersion()
	{
		return mActiveSyncVersion;
	}

	public void setActiveSyncVersion(String version)
	{
		mActiveSyncVersion = version;
	}

	public String getDomain()
	{
		return mDomain;
	}

	public void setDomain(String domain)
	{
		mDomain = domain;
	}

	public String getPolicyKey()
	{
		return mPolicyKey;
	}

	public void setPolicyKey(String policyKey)
	{
		this.mPolicyKey = policyKey;
	}

	public String getServerName()
	{
		return mServerName;
	}

	public void setServerName(String serverName)
	{
		this.mServerName = serverName;
	}

	public void setmUsername(String username)
	{
		this.mUsername = username;
	}

	public void setPassword(String password)
	{
		this.mPassword = password;
	}

	public WBXML getWbxml()
	{
		return wbxml;
	}

	public int getDeviceId()
	{
		return mDeviceId;
	}

	public void setDeviceId(int deviceId)
	{
		mDeviceId = deviceId;
	}

	/**
	 * Generates the auth string from the username, password and domain
	 */
	private void generateAuthString()
	{
		// For BPOS the DOMAIN is not required, so remove the backslash
		if (mDomain.equalsIgnoreCase(""))
			mAuthString = "Basic " + Utility.base64Encode(mUsername + ":" + mPassword);
		else
			mAuthString = "Basic "
							+ Utility.base64Encode(mDomain + "\\" + mUsername + ":" + mPassword);
	}

	/**
	 * Initializes the class by assigning the Exchange URL and the AuthString
	 * 
	 * @throws URISyntaxException
	 */
	public boolean Initialize()
	{
		wbxml = new WBXML();

		generateAuthString();

		Random rand = new Random();

		// Generate a random deviceId that is greater than 0
		while (mDeviceId <= 0)
			mDeviceId = rand.nextInt();

		// If we don't have a server name,
		// there is no way we can proceed
		if (mServerName.compareToIgnoreCase("") == 0)
			return false;

		// this is where we will send it
		try
		{
			URI uri = new URI((mUseSSL) ? "https" : "http", // Scheme
								mServerName, // Authority
								"/Microsoft-Server-ActiveSync", // path
								"User=" // query
										+ mUsername + "&DeviceId="
										+ mDeviceId
										+ "&DeviceType=Android", null // fragment
			);

			mUri = uri.toString();
		}
		catch (URISyntaxException e)
		{
			return false;
		}

		return true;
	}

	public ActiveSyncManager()
	{
	}

	public ActiveSyncManager(String email, String serverName, String domain, String username,
			String password, boolean useSSL, boolean acceptAllCerts, String policyKey,
			String activeSyncVersion, int deviceId)
	{
		mEmail = email;
		mServerName = serverName;
		mDomain = domain;
		mUsername = username;
		mPassword = password;
		mPolicyKey = policyKey;
		mActiveSyncVersion = activeSyncVersion;
		mUseSSL = useSSL;
		mAcceptAllCerts = acceptAllCerts;
		mDeviceId = deviceId;
	}

	/**
	 * @throws Exception
	 * @return Status code returned from the Exchange server
	 * 
	 *         Connects to the Exchange server and obtains the version of
	 *         ActiveSync supported by the server
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws ExchangeException 
	 */
	public int getExchangeServerVersion() throws ClientProtocolException, IOException, ExchangeException 
	{

		// First get the options from the server
		HttpResponse response = getOptions();

		// 200 indicates a success
		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode == 200)
		{

			Header[] headers = response.getHeaders("MS-ASProtocolVersions");

			if (headers.length != 0)
			{

				Header header = headers[0];

				// Parse out the ActiveSync Protocol version
				String versions = header.getValue();

				// Look for the last comma, and parse out the highest
				// version
				mActiveSyncVersion = versions.substring(versions.lastIndexOf(",") + 1);

				mActiveSyncVersionFloat = Float.parseFloat(mActiveSyncVersion);

				// Provision the device if necessary
				provisionDevice();
			}
		}
		return statusCode;
	}

	/**
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @param entity
	 *            The entity to decode
	 * @return The decoded WBXML or text/HTML entity
	 * 
	 *         Decodes the entity that is returned from the Exchange server
	 * @throws Exception
	 * @throws
	 */
	public String decodeContent(HttpEntity entity) throws IllegalStateException, IOException 
	{
		String result = "";

		if (entity != null)
		{
			java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

			// Parse all the entities
			String contentType = entity.getContentType().getValue();

			// WBXML entities
			if (contentType.compareToIgnoreCase("application/vnd.ms-sync.wbxml") == 0)
			{
				InputStream is = entity.getContent();
				InputStream bufIs = is;
				if(debug )
				{
					int contentLen = (int) entity.getContentLength();
					byte[] buf = new byte[(int)contentLen];
					int actLen = 0;
					int readed = 0;
					while((readed = is.read(buf, actLen, buf.length - actLen)) > 0)
						actLen +=readed;
					assert (actLen == contentLen);
					System.out.println("Received WBXML:");
					Hexdump.hexdump(System.out, buf, 0, buf.length);
					bufIs = new ByteArrayInputStream(buf);
				}
				
				wbxml.convertWbxmlToXml(bufIs, output);
				result = output.toString("UTF-8");

			}
			// Text / HTML entities
			else if (contentType.indexOf("text/") >=0)
			{
				InputStream is = entity.getContent();
				int contentLen = (int) entity.getContentLength();
				byte[] buf = new byte[(int)contentLen];
				int actLen = 0;
				int readed = 0;
				while((readed = is.read(buf, actLen, buf.length - actLen)) > 0)
					actLen +=readed;
				assert (actLen == contentLen);
				
				result = new String(buf, "UTF-8");
			
//				result = EntityUtils.toString(entity);
			}
		}
		// Log.d(TAG, (result.toString()));
		return result;

	}

	/**
	 * @param httpPost
	 *            The request to POST to the Exchange sever
	 * @return The response to the POST message
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * 
	 *             POSTs a message to the Exchange server. Any WBXML or String
	 *             entities that are returned by the server are parsed and
	 *             returned to the callee
	 */
	private HttpResponse sendPostRequest(HttpPost httpPost) throws ClientProtocolException, IOException
	{

		// POST the request to the server
		HttpClient client = getHttpClient();
		HttpContext localContext = new BasicHttpContext();
		HttpResponse rsp = client.execute(httpPost, localContext);
		if(rsp != null && rsp.getStatusLine().getStatusCode() == 451)
		{
			/*
			 * STATUS:HTTP/1.1 451 Redirect required
			 * X-MS-Location : https://blu-m.hotmail.com/Microsoft-Server-ActiveSync
			 */
			Header[] hs = rsp.getHeaders("X-MS-Location");
			if(hs != null && hs.length > 0)
			{
				String newUrl = hs[0].getValue();
				URI newUri;
				try
				{
					newUri = new URI(newUrl);
					URI u = httpPost.getURI();
					URI u2 = URIUtils.rewriteURI(u, URIUtils.extractHost(newUri));
					httpPost.setURI(u2);
					return sendPostRequest(httpPost);
				}
				catch (URISyntaxException e)
				{
					log.error("redirecting exchange:", e);
				}
			}
		}
		return rsp;
	}

	/**
	 * @param httpOptions
	 *            The OPTIONS message to send to the Exchange server
	 * @return The headers returned by the Exchange server
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * 
	 *             Sends an OPTIONS request to the Exchange server
	 */
	private HttpResponse sendOptionsRequest(HttpOptions httpOptions) throws ClientProtocolException, IOException 
	{

		// Send the OPTIONS message
		HttpClient client = getHttpClient();
		HttpContext localContext = new BasicHttpContext();
		return client.execute(httpOptions, localContext);
	}

	/**
	 * @return The headers returned by the Exchange server
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * 
	 *             Get the options that are supported by the Exchange server.
	 *             This is accomplished by sending an OPTIONS request with the
	 *             Cmd set to SYNC
	 */
	public HttpResponse getOptions() throws ClientProtocolException, IOException 
	{
		String uri = mUri;
		HttpResponse rsp = sendOptionsRequest(createHttpOptions(uri));
		Header[] hs = rsp.getHeaders("MS-ASProtocolVersions");
		if(hs.length > 0)
		{
			String[] versions = hs[0].getValue().split(",");
			setActiveSyncVersion(versions[versions.length-1]);
		}
		hs = rsp.getHeaders("MS-ASProtocolCommands");
		if(hs.length > 0)
		{
			String[] cmds = hs[0].getValue().split(",");
			for(String c:cmds)
				supportedCommands.add(c);
		}
		return rsp;
	}

	/**
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws ExchangeException 
	 * 
	 *             Send a Sync command to the Exchange server
	 */
	public HttpResponse sync(String folderId, String syncKey) throws ClientProtocolException, IOException, ExchangeException
	{
		String uri = mUri + "&Cmd=Sync";
		//@formatter:off
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
					+ "<Sync xmlns=\"AirSync:\">" 
						+ "<Collections>"
							+ "<Collection>" 
//								+ "<Class>Email</Class>" 
								+ "<SyncKey>" + syncKey	+ "</SyncKey>" 
								+ "<CollectionId>" + folderId + "</CollectionId>"
//								+ "<DeletesAsMoves/>" 
//								+ "<GetChanges/>"
								//+ "<Options> ... </Options>\r\n" 
							+ "</Collection>"
						+ "</Collections>" 
					+ "</Sync>";
		//@formatter:on
		return sendPostRequest(createHttpPost(uri, xml, true));
	}

	public HttpResponse syncMailHeader(int folderId, String syncKey) throws Exception
	{
		String uri = mUri + "&Cmd=Sync";
		//@formatter:off
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
					+ "<Sync xmlns=\"AirSync:\">" 
						+ "<Collections>"
							+ "<Collection>" 
//								+ "<Class>Email</Class>" 
								+ "<SyncKey>" + syncKey	+ "</SyncKey>" 
								+ "<CollectionId>" + folderId + "</CollectionId>"
								+ "<GetChanges>1</GetChanges>"
								+ "<WindowSize>20</WindowSize>"
								+ "<DeletesAsMoves/>" 
								+ "<Options>"  
								+ 	"<FilterType>0</FilterType>"  
								+ 	"<Conflict>1</Conflict>"  
								+ 	"<MIMETruncation>0</MIMETruncation>"  
								+ 	"<MIMESupport>0</MIMESupport>"  
								+ 	"<BodyPreference>"  
								+ 		"<Type>1</Type>"  
								+ 		"<TruncationSize>0</TruncationSize>"  
								+ 		"<AllOrNone>0</AllOrNone>"  
								+ 	"</BodyPreference>"  
								+ 	"<BodyPreference>"  
								+ 		"<Type>2</Type>"  
								+ 		"<TruncationSize>0</TruncationSize>"  
								+ 		"<AllOrNone>0</AllOrNone>"  
								+ 	"</BodyPreference>"  
								+ "</Options>" 
							+ "</Collection>"
						+ "</Collections>" 
					+ "</Sync>";
		//@formatter:on
		return sendPostRequest(createHttpPost(uri, xml, true));
	}
	/**
	 * @param syncKey
	 * @throws ExchangeException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws Exception
	 *             Send a FolderSync command to the Exchange server
	 */
	public HttpResponse folderSync(String syncKey) throws ClientProtocolException, IOException, ExchangeException 
	{
		// Create the request
		String uri = mUri + "&Cmd=FolderSync";
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
						+ "<FolderSync xmlns=\"FolderHierarchy:\">\n" + "\t<SyncKey>" + syncKey
						+ "</SyncKey>\n" + "</FolderSync>";

		// Send it to the server
		return sendPostRequest(createHttpPost(uri, xml, true));
	}

	/**
	 * @param query
	 *            The name to search the GAL for
	 * @param result
	 *            The XML contacts returned by the Exchange server
	 * 
	 * @return The status code returned from the Exchange server
	 * @throws ExchangeException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws Exception
	 * 
	 *             This method searches the GAL on the Exchange server
	 */
	public int searchGAL(String query, StringBuffer result) throws ClientProtocolException, IOException, ExchangeException 
	{
		// Create the request
		String uri = mUri + "&Cmd=Search";

		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
						+ "<Search xmlns=\"Search:\">\n" + "\t<Store>\n" + "\t\t<Name>GAL</Name>\n"
						+ "\t\t<Query>" + query + "</Query>\n" + "\t\t<Options>\n"
						+ "\t\t\t<Range>0-99</Range>\n" + "\t\t</Options>\n" + "\t</Store>\n"
						+ "</Search>";

		// Send it to the server
		HttpResponse response = sendPostRequest(createHttpPost(uri, xml, true));

		// Check the response code to see if the result was 200
		// Only then try to decode the content

		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode == 200)
		{
			// Decode the XML content
			result.append(decodeContent(response.getEntity()));
		}

		// parse and return the results
		return statusCode;
	}

	public boolean isSupportedCommand(String cmd)
	{
		return supportedCommands.contains(cmd);
	}
	/**
	 * @throws ExchangeException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws Exception
	 * 
	 *             Sends a Provision command to the Exchange server. Only needed
	 *             for Exchange 2007 and above
	 */
	public void provisionDevice() throws ClientProtocolException, IOException, ExchangeException 
	{

		if(!isSupportedCommand("Provision"))
			return;
		// Create the request
		String uri = mUri + "&Cmd=Provision";
		String policyType;

		//When the value of the MS-ASProtocolVersion header ([MS-ASHTTP] section 2.2.1.1.2.4) is 14.1, 
		//the client MUST send the settings:DeviceInformation element with its contents when sending an 
		//initial Provision command request to the server but not on subsequent requests. 
		
		if (getActiveSyncVersionFloat() >= 12.0)
			policyType = "MS-EAS-Provisioning-WBXML";
		else
			policyType = "MS-WAP-Provisioning-XML";

		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n<Provision xmlns=\"Provision:\" xmlns:settings=\"Settings:\">\n");
		if(getActiveSyncVersionFloat() >= 14.1)
		{
			sb.append("<settings:DeviceInformation>\r\n" + 
					"      <settings:Set>" + 
					"         <settings:Model>Android</settings:Model>" + 
					//"         <settings:IMEI>...</settings:IMEI>\r\n" + 
					//"         <settings:FriendlyName>...</settings:FriendlyName>\r\n" + 
					"         <settings:OS>Android11</settings:OS>" + 
					"         <settings:OSLanguage>en_US</settings:OSLanguage>" + 
					//"         <settings:PhoneNumber>...</settings:PhoneNumber>\r\n" + 
					//"         <settings:MobileOperator>...</settings:MobileOperator>\r\n" + 
					//"         <settings:UserAgent>...</settings:UserAgent>\r\n" + 
					"      </settings:Set>" + 
					"   </settings:DeviceInformation>");
		}
		sb.append( "\t<Policies>\n"
						+ "\t\t<Policy>\n" + "\t\t\t<PolicyType>" + policyType + "</PolicyType>\n"
						+ "\t\t</Policy>\n" + "\t</Policies>\n" + "</Provision>");

		HttpResponse response = sendPostRequest(createHttpPost(uri, sb.toString(), true));
		
		try
		{
			Document doc = getParser(response, "Provision", "C:\\temp\\provision0.xml");
			Element root =doc.getRootElement();
			int status = XmlDomHelper.getChildAsInt(root, "Status", EXCH_STATUS_OK);
			if(status != EXCH_STATUS_OK)
				throw new ExchangeException("Fail to provision, status:"+status);
			if(root.getElement(null, "RemoteWipe") != null)
			{
				//do remote wipe
			}
			Element policyElement = XmlDomHelper.searchElement(root, "Policies", "Policy");
			String tempPolicyKey = XmlDomHelper.getChildAsString(policyElement, "PolicyKey");
			int pstatus = XmlDomHelper.getChildAsInt(policyElement, "Status");
			/*Status element is a child of the Policy element and indicates whether the policy settings were applied correctly.
			 	1 Success The Data element contains the policy settings to apply.
				2 No policy The server doesn't have a policy. The device doesn't need to submit a second request; 
					it can proceed to sync data.
				3 Unknown policy type The device sent an invalid value for the PolicyType element. The only valid 
					value for the PolicyType element is "MS-EAS-Provisioning-WBXML".
				4 Policy data is corrupt The policy data on the server is corrupted. The device should suggest that
				 	the user contact the administrator.
				5 Policy key mismatch The policy key sent in the last request does not match the key stored on the 
					server. Either the device sent the wrong policy key or the policy changed on the server since 
					the first response. The device should re-initiate the provisioning process from the beginning.
			 */
			if(pstatus == 2)
			{//
				return;
			}
			if(Utils.isEmpty(tempPolicyKey))
				throw new ExchangeException("Fail get temporary policy key");
			
			/* initial provision get response like
			 <?xml version='1.0' encoding='utf-8' ?>
<Provision xmlns="Provision">
  <DeviceInformation>
    <Status>1</Status>
  </DeviceInformation>
  <Status>1</Status>
  <Policies>
    <Policy>
      <PolicyType>MS-EAS-Provisioning-WBXML</PolicyType>
      <Status>1</Status>
      <PolicyKey>1310517184</PolicyKey>
      <Data>
        <EASProvisionDoc>
          <DevicePasswordEnabled>0</DevicePasswordEnabled>
          <AlphanumericDevicePasswordRequired>0</AlphanumericDevicePasswordRequired>
          <PasswordRecoveryEnabled>0</PasswordRecoveryEnabled>
          <DeviceEncryptionEnabled>0</DeviceEncryptionEnabled>
          <AttachmentsEnabled>1</AttachmentsEnabled>
          <MinDevicePasswordLength />
          <MaxInactivityTimeDeviceLock />
          <MaxDevicePasswordFailedAttempts />
          <MaxAttachmentSize />
          <AllowSimpleDevicePassword>1</AllowSimpleDevicePassword>
          <DevicePasswordExpiration />
          <DevicePasswordHistory>0</DevicePasswordHistory>
          <AllowStorageCard>1</AllowStorageCard>
          <AllowCamera>1</AllowCamera>
          <RequireDeviceEncryption>0</RequireDeviceEncryption>
          <AllowUnsignedApplications>1</AllowUnsignedApplications>
          <AllowUnsignedInstallationPackages>1</AllowUnsignedInstallationPackages>
          <MinDevicePasswordComplexCharacters>1</MinDevicePasswordComplexCharacters>
          <AllowWiFi>1</AllowWiFi>
          <AllowTextMessaging>1</AllowTextMessaging>
          <AllowPOPIMAPEmail>1</AllowPOPIMAPEmail>
          <AllowBluetooth>2</AllowBluetooth>
          <AllowIrDA>1</AllowIrDA>
          <RequireManualSyncWhenRoaming>0</RequireManualSyncWhenRoaming>
          <AllowDesktopSync>1</AllowDesktopSync>
          <MaxCalendarAgeFilter>0</MaxCalendarAgeFilter>
          <AllowHTMLEmail>1</AllowHTMLEmail>
          <MaxEmailAgeFilter>0</MaxEmailAgeFilter>
          <MaxEmailBodyTruncationSize>-1</MaxEmailBodyTruncationSize>
          <MaxEmailHTMLBodyTruncationSize>-1</MaxEmailHTMLBodyTruncationSize>
          <RequireSignedSMIMEMessages>0</RequireSignedSMIMEMessages>
          <RequireEncryptedSMIMEMessages>0</RequireEncryptedSMIMEMessages>
          <RequireSignedSMIMEAlgorithm>0</RequireSignedSMIMEAlgorithm>
          <RequireEncryptionSMIMEAlgorithm>0</RequireEncryptionSMIMEAlgorithm>
          <AllowSMIMEEncryptionAlgorithmNegotiation>2</AllowSMIMEEncryptionAlgorithmNegotiation>
          <AllowSMIMESoftCerts>1</AllowSMIMESoftCerts>
          <AllowBrowser>1</AllowBrowser>
          <AllowConsumerEmail>1</AllowConsumerEmail>
          <AllowRemoteDesktop>1</AllowRemoteDesktop>
          <AllowInternetSharing>1</AllowInternetSharing>
          <UnapprovedInROMApplicationList />
          <ApprovedApplicationList />
        </EASProvisionDoc>
      </Data>
    </Policy>
  </Policies>
</Provision>
			 */
			
			
			// Now that we have the temporary policy key,
			// Tell the server that we accept all the provisioning settings by
			// Setting the status to 1
			String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<Provision xmlns=\"Provision:\">\n"
					+ "\t<Policies>\n" + "\t\t<Policy>\n" + "\t\t\t<PolicyType>" + policyType
					+ "</PolicyType>\n" + "\t\t\t<PolicyKey>" + tempPolicyKey + "</PolicyKey>\n"
					+ "\t\t\t<Status>1</Status>\n" + "\t\t</Policy>\n" + "\t</Policies>\n"
					+ "</Provision>";

			response = sendPostRequest(createHttpPost(uri, xml, false));
			doc = getParser(response, "Provision", "C:\\temp\\provision1.xml");
			root =doc.getRootElement();
			/* response of second Provision like:
			 <?xml version="1.0" encoding="utf-8"?>
<Provision xmlns="Provision:">
   <Status>1</Status>
   <Policies>
      <Policy>
         <PolicyType> MS-EAS-Provisioning-WBXML </PolicyType>
         <Status>1</Status> 
         <PolicyKey>3942919513</PolicyKey>
      </Policy>
   </Policies>
</Provision>
			 */
			status = XmlDomHelper.getChildAsInt(root, "Status", EXCH_STATUS_OK);
			if(status != EXCH_STATUS_OK)
				throw new ExchangeException("Fail to provision phase two, status:"+status);
			
			policyElement = XmlDomHelper.searchElement(root, "Policies", "Policy");

			// Get the final policy key
			mPolicyKey = XmlDomHelper.getChildAsString(policyElement, "PolicyKey");

		}
		catch (IllegalStateException | XmlPullParserException | NoElementException e)
		{
			throw new ExchangeException(e);
		}
		
	}

	/**
	 * @return the HttpClient object
	 * 
	 *         Creates a HttpClient object that is used to POST messages to the
	 *         Exchange server
	 */
	private HttpClient getHttpClient()
	{
		if(httpclient != null)
			return httpclient;
		HttpParams httpParams = new BasicHttpParams();

		// Turn off stale checking. Our connections break all the time anyway,
		// and it's not worth it to pay the penalty of checking every time.
		HttpConnectionParams.setStaleCheckingEnabled(httpParams, false);

		// Default connection and socket timeout of 120 seconds. Tweak to taste.
		HttpConnectionParams.setConnectionTimeout(httpParams, 120 * 1000);
		HttpConnectionParams.setSoTimeout(httpParams, 120 * 1000);
		HttpConnectionParams.setSocketBufferSize(httpParams, 131072);
		httpParams.setParameter("http.protocol.handle-redirects",true);
		
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		registry.register(new Scheme("https", mAcceptAllCerts	? new FakeSocketFactory()
																: SSLSocketFactory
																		.getSocketFactory(), 443));
		httpclient = new DefaultHttpClient(new PoolingClientConnectionManager(registry),httpParams);

		// Set the headers
		httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
											HttpVersion.HTTP_1_1);
		httpclient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Android");

		// Make sure we are not validating any hostnames
		SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		return httpclient;
	}

	private HttpPost createHttpPost(String uri, String requestXML) throws Exception
	{
		return createHttpPost(uri, requestXML, false);
	}

	/**
	 * @param uri
	 *            The URI to send the POST message to
	 * @param requestXML
	 *            The XML to send in the message
	 * @param includePolicyKey
	 *            Should we include the policyKey in the header
	 * @return The POST request that can be sent to the server
	 * @throws ExchangeException 
	 * @throws Exception
	 * 
	 *             Creates a POST request that can be sent to the Exchange
	 *             server. This method sets all the necessary headers in the
	 *             POST message that are required for the Exchange server to
	 *             respond appropriately
	 */
	private HttpPost createHttpPost(String uri, String requestXML, boolean includePolicyKey) throws ExchangeException
	{

		// Set the common headers
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setHeader("User-Agent", "EASClient/2.1300");
		httpPost.setHeader("Accept", "*/*");
		httpPost.setHeader("Content-Type", "application/vnd.ms-sync.wbxml");

		// If we are connecting to Exchange 2010 or above
		// Lets tell the Exchange server that we are a 12.1 client
		// This is so we don't have to support sending of additional
		// information in the provision method
//		if (getActiveSyncVersionFloat() >= 14.0)
//			httpPost.setHeader("MS-ASProtocolVersion", "12.1");
//		else		// Else set the version to the highest version returned by the Exchange server
			httpPost.setHeader("MS-ASProtocolVersion", getActiveSyncVersion());

		// Log.d(TAG, mActiveSyncVersion);
		httpPost.setHeader("Accept-Language", "en-us");
		httpPost.setHeader("Authorization", mAuthString);

		// Include policy key if required
		if (includePolicyKey)
			httpPost.setHeader("X-MS-PolicyKey", mPolicyKey);

		// Add the XML to the request
		if (requestXML != null)
		{
			byte[] bytes = xmlToWbxml(requestXML);
			log.debug("Send request XML:\n{}",requestXML);
//			System.out.println("WBXML:");
//			Hexdump.hexdump(System.out, bytes, 0, bytes.length);
			
			ByteArrayEntity myEntity = new ByteArrayEntity(bytes);
			myEntity.setContentType("application/vnd.ms-sync.wbxml");
			httpPost.setEntity(myEntity);
		}
		return httpPost;
	}

	public byte[] xmlToWbxml(String requestXML) throws ExchangeException
	{
		// Log.d(TAG, requestXML);

		// Convert the XML to WBXML
		ByteArrayInputStream xmlParseInputStream;
		try
		{
			xmlParseInputStream = new ByteArrayInputStream(requestXML.getBytes("UTF-8"));

			java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
			//method 1, use code from corporateaddressbook
			wbxml.convertXmlToWbxml(xmlParseInputStream, output);
		
			//method 2, use code from kxml
			//this method will output a string table in wbxml, guess this is not accepted by 
	//		KXmlParser parser = new KXmlParser();
	//		parser.setInput(xmlParseInputStream, "UTF-8");
	//		Document dom = new Document();
	//		dom.parse(parser);
	//		 
	//		WbxmlSerializer ser = new WbxmlSerializer();
	//		for(int i=0;i<Tags.pages.length;i++)
	//			ser.setTagTable(i, Tags.pages[i]);
	//		
	//		ser.setOutput(output, null);
	//		dom.write(ser);
	//		ser.flush();
			 
			
			//method 3, use k9mail
	//		k9WBXML.getWbxml().convertXmlToWbxml(xmlParseInputStream, output);
	
			byte[] b = output.toByteArray();
			return b;
		}
		catch (UnsupportedEncodingException e)
		{
			throw new ExchangeException(e);
		}
	}

	/**
	 * @param uri
	 *            The URI that the request needs to be sent to
	 * @return The OPTIONS request that can be sent to the server
	 * 
	 *         This method creates an OPTIONS request that can be sent to the
	 *         Exchange server to query for the features that are supported by
	 *         the server
	 */
	private HttpOptions createHttpOptions(String uri)
	{
		HttpOptions httpOptions = new HttpOptions(uri);
		httpOptions.setHeader("User-Agent", "Android");
		httpOptions.setHeader("Authorization", mAuthString);

		return httpOptions;
	}

	/**
	 * @param xml
	 *            The XML to parse
	 * @param nodeName
	 *            The Node to search for in the XML
	 * @return List of strings found in the specified node
	 * @throws ExchangeException 
	 * 
	 *             This method parses the an XML string and returns all values
	 *             that were found in the node specified in the request
	 */
	public String[] parseXML(String xml, String nodeName) throws ExchangeException
	{
		// Our parser does not handle ampersands too well. Replace with &amp;
		xml = xml.replaceAll("&", "&amp;");

		// Parse the XML
		ByteArrayInputStream xmlParseInputStream = new ByteArrayInputStream(xml.toString()
				.getBytes());
		XMLReader xr;
		try
		{
			xr = XMLReaderFactory.createXMLReader();
			XMLParser parser = new XMLParser(nodeName);
			xr.setContentHandler(parser);
			xr.parse(new InputSource(xmlParseInputStream));
			return parser.getOutput();
		}
		catch (SAXException | IOException e)
		{
			throw new ExchangeException(e);
		}
		
	
	}

	public HttpResponse autoDiscover() throws Exception
	{
		String uri = mUri + "&Cmd=Audodiscover";
		//@formatter:off
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
					+ "<Autodiscover>" 
						+ "<EMailAddress>" + mEmail + "</EMailAddress>" 
					+ "</Autodiscover>";
		//@formatter:on

		// Set the common headers
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setHeader("User-Agent", "Android");
		httpPost.setHeader("Accept", "*/*");
		httpPost.setHeader("Content-Type", "application/vnd.ms-sync.wbxml");

		// If we are connecting to Exchange 2010 or above
		// Lets tell the Exchange server that we are a 12.1 client
		// This is so we don't have to support sending of additional
		// information in the provision method
		if (getActiveSyncVersionFloat() >= 14.0)
			httpPost.setHeader("MS-ASProtocolVersion", "12.1");
		// Else set the version to the highest version returned by the
		// Exchange server
		else
			httpPost.setHeader("MS-ASProtocolVersion", getActiveSyncVersion());

		// Log.d(TAG, mActiveSyncVersion);
		httpPost.setHeader("Accept-Language", "en-us");
		httpPost.setHeader("Authorization", mAuthString);

		ByteArrayEntity myEntity = new ByteArrayEntity(xml.getBytes("UTF-8"));
		myEntity.setContentType("text/xml");
		httpPost.setEntity(myEntity);

		return sendPostRequest(httpPost);

	}

	public HttpResponse retrieveMail(String folderId, String mailId) throws ExchangeException,  IOException 
	{
		/**
		 * airsyncbase:Type = 1, get mail in plaint text
		 * airsyncbase:Type = 2, get mail in HTML
		 * 					  3,	RTF
		 * 					  4,	MIME
		 */
		String uri = mUri + "&Cmd=ItemOperations";
		//@formatter:off
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
//				+"<Sync xmlns:airsyncbase=\"AirSyncBase:\" xmlns=\"AirSync:\">"
//				 + "<Collections>" 
//				 + "<Collection>" 
//				 + "<SyncKey>"+syncKey+"</SyncKey>"  
//				 + "<CollectionId>" + folderId+"</CollectionId>"  
//				 + "<DeletesAsMoves>1</DeletesAsMoves>"  
//				 + "<GetChanges>1</GetChanges>"  
//				 + "<WindowSize>512</WindowSize>"  
//				 + "<Options>"  
//				 + "<MIMESupport>1</MIMESupport>"  
//				 + "<airsyncbase:BodyPreference>"  
//				 + "<airsyncbase:Type>2</airsyncbase:Type>"   //type 2 means HTML, 1 for plain text, 4 for MIME
////				 + "<airsyncbase:TruncationSize>5120</airsyncbase:TruncationSize>"  
//				 + "</airsyncbase:BodyPreference>"  
//				 + "</Options>"
//				 + "</Collection>"  
//				 + "</Collections>"  
//				 + "</Sync>"

//					+ "<ItemOperations xmlns:airsync=\"AirSync:\">" 
//						+"<Fetch>"
//							+ "<airsync:ServerId>" + mailId + "</airsync:ServerId>" 
//						+"</Fetch>"
//					+ "</ItemOperations>"
				
				
				
//				+ "<ItemOperations xmlns:airsync=\"AirSync:\" xmlns:airsyncbase=\"AirSyncBase:\" xmlns=\"ItemOperations:\">"
//  + "<Fetch>"
//    + "<Store>Mailbox</Store>"
//    + "<airsync:CollectionId>" + folderId +"</airsync:CollectionId>"
//    + "<airsync:ServerId>" + mailId + "</airsync:ServerId>"
//    + "<Options>"
//      + "<airsync:MIMESupport>1</airsync:MIMESupport>"
//      + "<airsyncbase:BodyPreference>"            
//        + "<airsyncbase:Type>2</airsyncbase:Type>"
//        + "<airsyncbase:TruncationSize>5120</airsyncbase:TruncationSize>"
//        + "<airsyncbase:AllOrNone>0</airsyncbase:AllOrNone>"
//      + "</airsyncbase:BodyPreference>"
//    + "</Options>"
//  + "</Fetch>"
//+ "</ItemOperations>"

				+"<ItemOperations xmlns=\"ItemOperations\" xmlns:airsync=\"AirSync:\" xmlns:airsyncbase=\"AirSyncBase:\"><Fetch><Store>Mailbox</Store><airsync:CollectionId>" + folderId + "</airsync:CollectionId><airsync:ServerId>" + mailId + "</airsync:ServerId><Options><airsyncbase:BodyPreference><airsyncbase:Type>2</airsyncbase:Type><airsyncbase:AllOrNone>0</airsyncbase:AllOrNone></airsyncbase:BodyPreference></Options></Fetch></ItemOperations>"
				
				
						;
		//@formatter:on
		HttpPost post = createHttpPost(uri, xml,true);
//		post.addHeader("MS-ASAcceptMultiPart", "T");
		return sendPostRequest(post);
	}
	public HttpResponse retrieveAttachment(String folderId, String mailId, String fileReference) throws ExchangeException,  IOException 
	{
		String uri = mUri + "&Cmd=ItemOperations";
		//@formatter:off
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"


				//+"<ItemOperations xmlns=\"ItemOperations\" xmlns:airsync=\"AirSync:\" xmlns:airsyncbase=\"AirSyncBase:\"><Fetch><Store>Mailbox</Store><airsync:CollectionId>" + folderId + "</airsync:CollectionId><airsync:ServerId>" + mailId + "</airsync:ServerId><airsyncbase:FileReference>"+fileReference+"</airsyncbase:FileReference></Fetch></ItemOperations>"
				+"<ItemOperations xmlns=\"ItemOperations\" xmlns:airsync=\"AirSync:\" xmlns:airsyncbase=\"AirSyncBase:\"><Fetch><Store>Mailbox</Store><airsyncbase:FileReference>"+fileReference+"</airsyncbase:FileReference></Fetch></ItemOperations>"
				
						;
		//@formatter:on
		HttpPost post = createHttpPost(uri, xml,true);
//		post.addHeader("MS-ASAcceptMultiPart", "T");
		return sendPostRequest(post);
	}

	public boolean checkOpen()
	{
		return true;
	}
	
	private Document dump(String name, HttpResponse rsp, String xmlFile) 
	{
		try
		{
			System.out.println(new Date().toString() + name);
			System.out.println("STATUS:"+rsp.getStatusLine());
			for (Header h : rsp.getAllHeaders())
			{
				System.out.println(h.getName() + " : " + h.getValue());
			}
	
			String xml = null;
			HttpEntity entity = rsp.getEntity();
			long contentLen = entity.getContentLength();
			if (entity != null && contentLen > 0 )
			{
				xml = decodeContent(entity);
				XmlPullParserFactory f  = XmlPullParserFactory.newInstance();
				XmlPullParser p = f.newPullParser();
				
				p.setInput(new StringReader(xml));
				Document d = new Document();
				d.parse(p);
				KXmlSerializer ks = new KXmlSerializer();
				StringWriter w = new StringWriter(4096);
				ks.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
				ks.setOutput(w);
				d.write(ks);
				xml = w.toString();
				System.out.println(xml);
				FileOutputStream fo = new FileOutputStream(xmlFile);
				fo.write(xml.getBytes("UTF-8"));
				fo.close();
				
				if(!isSuccessed(rsp))
					return null;
				return d;
//				XmlPullParser p2 = f.newPullParser();
//				
//				p2.setInput(new StringReader(xml));
//				return p2;
				
				// }
				// else
				// {
				//
				// byte[] buf = new byte[(int)contentLen];
				// int actLen = 0;
				// int readed = 0;
				// while((readed = is.read(buf, actLen, buf.length - actLen)) > 0)
				// actLen +=readed;
				// assert (actLen == contentLen);
				//
				// System.out.println(new String(buf, "UTF-8"));
				// FileOutputStream fo = new FileOutputStream(xmlFile);
				// fo.write(buf);
				//
				// }
			}
			return null;
		}
		catch(Exception ex)
		{
			log.debug("Fail dump", ex);
		}
		return null;
	}
	public static boolean isSuccessed(HttpResponse rsp)
	{
		int statusCode = rsp.getStatusLine().getStatusCode();
		
		log.error("Status code:{}", statusCode);
		return statusCode == HttpStatus.SC_OK;
	}
	Document getXmlBody(HttpResponse rsp) throws IllegalStateException, IOException, XmlPullParserException
	{
		
		if(!ActiveSyncManager.isSuccessed(rsp))
			return null;
		HttpEntity entity = rsp.getEntity();
		long contentLen = entity.getContentLength();
		if (entity != null && contentLen > 0 )
		{
			WbxmlParser p = new WbxmlParser();
			p.setInput(entity.getContent(), "UTF-8");
			Document d = new Document();
			d.parse(p);
			
			return d;
		}
		return null;
	}
	static boolean dump=true;
	public Document getParser(HttpResponse rsp, String name, String outFile) throws IllegalStateException, IOException, XmlPullParserException
	{
		if(dump)
			return dump(name, rsp, outFile);
		else
			return getXmlBody(rsp);
		
	}
	XmlPullParser getXmlPullParser(HttpResponse rsp)throws IllegalStateException, IOException, XmlPullParserException
	{
		WbxmlParser p = new WbxmlParser();
		if(!ActiveSyncManager.isSuccessed(rsp))
			return null;
		HttpEntity entity = rsp.getEntity();
		long contentLen = entity.getContentLength();
		if (entity != null && contentLen > 0 )
		{
			
			p.setInput(entity.getContent(), "UTF-8");
			return p;
		}
		return null;
	}

}
