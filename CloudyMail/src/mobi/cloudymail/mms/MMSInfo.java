package mobi.cloudymail.mms;
import java.io.File;

import mobi.cloudymail.util.Utils;
import android.content.Context;
import android.net.Uri;

import com.google.android.mms.pdu.CharacterSets;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.SendReq;

/**
* @author 
* @version 创建时间：2012-1-31 下午01:59:30
*/
public class MMSInfo {
        private Context con;
        private PduBody pduBody;
        //以分号分隔的手机号字符串
        private String recieverNum;
        private int partCount = 1;
        private String subject="";
		//        private static final String SUBJECT_STR = "来自XX好友的彩信"; // 彩信主题
        public void setSubject(String subject)
        {
        	this.subject=subject;
        }
        public MMSInfo(Context con, String recieverNum) {
                // TODO Auto-generated constructor stub
                this.con = con;
                this.recieverNum = recieverNum;
                pduBody = new PduBody();
        }

	/**
	 * @param type
	 * @param str
	 * 
	 */
	public void addPart(String type, String str)
	{
		if (Utils.isEmpty(type) || Utils.isEmpty(str))
			return;
		if (type.equals("text"))
		{
			PduPart partPdu3 = new PduPart();
			partPdu3.setCharset(CharacterSets.UTF_8);
			partPdu3.setName("mms_text.txt".getBytes());
			partPdu3.setContentType("text/plain".getBytes());
			partPdu3.setData(str.getBytes());
			pduBody.addPart(partPdu3);
		}
		if (type.equals("image"))
		{
			PduPart partPdu = new PduPart();
			partPdu.setCharset(CharacterSets.UTF_8);
			partPdu.setName("camera.jpg".getBytes());
			partPdu.setContentType("image/png".getBytes());
			partPdu.setDataUri(Uri.fromFile(new File(str)));
			pduBody.addPart(partPdu);
		}
		if (type.equals("audio"))
		{
			PduPart partPdu2 = new PduPart();
			partPdu2.setCharset(CharacterSets.UTF_8);
			partPdu2.setName("speech_test.amr".getBytes());
			partPdu2.setContentType("audio/amr".getBytes());
			// partPdu2.setContentType("audio/amr-wb".getBytes());
			// partPdu2.setDataUri(Uri.parse("file://mnt//sdcard//.lv//audio//1326786209801.amr"));
			partPdu2.setDataUri(Uri.fromFile(new File(str)));
			pduBody.addPart(partPdu2);
		}
		// if("jpg".equals(getTypeFromUri(uriStr)))
		// part.setContentType("image/jpg".getBytes());
		// else if("png".equals(getTypeFromUri(uriStr)))
		// part.setContentType("image/png".getBytes());
	}

        /**
         * 通过URI路径得到图片格式，如："file://mnt/sdcard//1.jpg" -----> "jpg"
         * 
         * @author 
         * @param uriStr
         * @return
         */
        private String getTypeFromUri(String uriStr) {
                return uriStr.substring(uriStr.lastIndexOf("."), uriStr.length());
        }

        /**
         * 将彩信的内容以及主题等信息转化成byte数组，准备通过http协议发送到"http://mmsc.monternet.com"
         * 
         * @author 邓
         * @return
         */
        public byte[] getMMSBytes() {
                PduComposer composer = new PduComposer(con, initSendReq());
                return composer.make();
        }

        /**
         * 初始化SendReq
         * 
         * @author 
         * @return
         */
        private SendReq initSendReq() {
                SendReq req = new SendReq();
                EncodedStringValue[] sub = EncodedStringValue.extract(subject);
                if (sub != null && sub.length > 0) {
                        req.setSubject(sub[0]);// 设置主题
                }
                EncodedStringValue[] rec = EncodedStringValue.extract(recieverNum);
                if (rec != null && rec.length > 0) {
                	for(int i=0;i<rec.length;i++)
                        req.addTo(rec[i]);// 设置接收者
                }
		
                req.setBody(pduBody);
                return req;
        }

}