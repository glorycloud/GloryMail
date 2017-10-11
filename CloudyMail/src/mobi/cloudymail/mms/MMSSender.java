//发送类
package mobi.cloudymail.mms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mobi.cloudymail.mailclient.R;
import mobi.cloudymail.util.Utils;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * 发送彩信至彩信中心
 */
public class MMSSender
{
	private static final String TAG = "MMSSender";
	public static String mmscUrl = "http://mmsc.monternet.com";
	public static String mmsProxy = "10.0.0.172";
	public static int mmsProt = 80;
	// 电信彩信中心url，代理，端口
	public static String mmscUrl_ct = "http://mmsc.vnet.mobi";//apn::ctwap
	public static String mmsProxy_ct = "10.0.0.200";
	// 移动彩信中心url，代理，端口
	public static String mmscUrl_cm = "http://mmsc.monternet.com";//apn:cmwap
	public static String mmsProxy_cm = "010.000.000.172";
	// 联通彩信中心url，代理，端口
	public static String mmscUrl_uni = "http://mmsc.myuni.com.cn";   //http://mmsc.vnet.mobi apn:uniwap或3gwap
	public static String mmsProxy_uni = "10.0.0.172";
	private static String HDR_VALUE_ACCEPT_LANGUAGE = "";
	private static final String HDR_KEY_ACCEPT = "Accept";
	private static final String HDR_KEY_ACCEPT_LANGUAGE = "Accept-Language";
	private static final String HDR_VALUE_ACCEPT = "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic";
	private static String APN_NET_ID = null;
	public List<String> getSimMNC(Context context)
	{
		TelephonyManager telManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imsi = telManager.getSubscriberId();
		if (imsi != null)
		{
			ArrayList<String> list = new ArrayList<String>();
			if (imsi.startsWith("46000") || imsi.startsWith("46002")||imsi.startsWith("46007"))
			{
				// 因为移动网络编号46000下的IMSI已经用完，所以虚拟了一个46002编号，134/159号段使用了此编号
				// 中国移动
				list.add(mmscUrl_cm);
				list.add(mmsProxy_cm);
			}
			else if (imsi.startsWith("46001"))
			{
				// 中国联通
				list.add(mmscUrl_uni);
				list.add(mmsProxy_uni);
			}
			else if (imsi.startsWith("46003"))
			{
				// 中国电信
				list.add(mmscUrl_ct);
				list.add(mmsProxy_ct);
			}
//			shouldChangeApn(context);
			return list;
		}
		return null;
	}

	public boolean shouldChangeApn(final Context context)
	{

		final String wapId = getWapApnId(context);
		String apnId = getCurrentApn(context);
		// 若当前apn不是wap，则切换至wap
		if (!wapId.equals(apnId))
		{
			APN_NET_ID = apnId;
			setApn(context, wapId);
			// 切换apn需要一定时间，先让等待2秒
			try
			{
				Thread.sleep(2000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

	private static String getCurrentApn(Context context)
	{
		ContentResolver resoler = context.getContentResolver();
		String[] projection = new String[] { "_id" };
		Cursor cur = resoler.query(	Uri.parse("content://telephony/carriers/preferapn"),//取得当前设置的apn
									projection,
									null, null, null);
		String apnId = null;
		if (cur != null && cur.moveToFirst())
		{
			do
			{
				apnId = cur.getString(cur.getColumnIndex("_id"));
			} while (cur.moveToNext());
		}
		return apnId;
	}

	/**
	 * 设置接入点
	 * 
	 * @param id
	 */
	private static void setApn(Context context, String id)
	{
		Uri uri = Uri.parse("content://telephony/carriers/preferapn");
		ContentResolver resolver = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put("apn_id", id);
		resolver.update(uri, values, null, null);
	}

	

	/**
	 * 取到wap接入點的id
	 * 
	 * @return
	 */
	private static String getWapApnId(Context context)
	{
		ContentResolver contentResolver = context.getContentResolver();
		String[] projection = new String[] { "_id", "proxy" };
		Cursor cur = contentResolver.query(	Uri.parse("content://telephony/carriers"), projection,//取得所有的apn列表
											"current = 1", null, null);
		if (cur != null && cur.moveToFirst())
		{
			do
			{
				String id = cur.getString(0);
				String proxy = cur.getString(1);
				if (!Utils.isEmpty(proxy))
				{
					return id;
				}
			} while (cur.moveToNext());
		}
		return null;
	}

	public boolean sendMMS(List<String> list, final Context context, byte[] pdu) throws Exception
	{
		// HDR_AVLUE_ACCEPT_LANGUAGE = getHttpAcceptLanguage();
		if (list == null)
		{
			new Handler().post(new Runnable() {

				@Override
				public void run()
				{
					Toast.makeText(context, context.getResources().getString(R.string.noSimCard), Toast.LENGTH_LONG).show();
				}
			});
			return false;
		}
		String mmsUrl = (String) list.get(0);
		String mmsProxy = (String) list.get(1);
		HttpClient client = null;
		try
		{
			HttpHost httpHost = new HttpHost(mmsProxy, 80);
			HttpParams httpParams = new BasicHttpParams();
			httpParams.setParameter(ConnRouteParams.DEFAULT_PROXY, httpHost);
			HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
			client = new DefaultHttpClient(httpParams);
			
			HttpPost post = new HttpPost(mmsUrl);
			// mms PUD START
			ByteArrayEntity entity = new ByteArrayEntity(pdu);
			entity.setContentType("application/vnd.wap.mms-message");
			post.setEntity(entity);
			post.addHeader(HDR_KEY_ACCEPT, HDR_VALUE_ACCEPT);
			post.addHeader(HDR_KEY_ACCEPT_LANGUAGE, HDR_VALUE_ACCEPT_LANGUAGE);
			post.addHeader(	"user-agent",
							"Mozilla/5.0(Linux;U;Android 2.1-update1;zh-cn;ZTE-C_N600/ZTE-C_N600V1.0.0B02;240*320;CTC/2.0)AppleWebkit/530.17(KHTML,like Gecko) Version/4.0 Mobile Safari/530.17");
			// mms PUD END
			HttpParams params = client.getParams();
			HttpProtocolParams.setContentCharset(params, "UTF-8");
			HttpResponse response = client.execute(post);
			StatusLine status = response.getStatusLine();
			Log.d(TAG, "status " + status.getStatusCode());
			if (status.getStatusCode() != 200)
			{
				throw new IOException("HTTP error: " + status.getReasonPhrase());
			}
			// 彩信发送完毕后检查是否需要把接入点切换回来
			if (null != APN_NET_ID)
			{
				setApn(context, APN_NET_ID);
			}
			return true;

		}
		catch (Exception e)
		{
			Log.d(Utils.LOGTAG, "Fail send MMS：" , e);
			// 发送失败处理
		}
		return false;
	}
}
