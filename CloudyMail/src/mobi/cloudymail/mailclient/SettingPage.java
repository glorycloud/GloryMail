package mobi.cloudymail.mailclient;

import static mobi.cloudymail.util.Utils.LOGTAG;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.NewDbHelper;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;


public class SettingPage extends PreferenceActivity implements
		OnPreferenceChangeListener
{
	private String _signature_content = null;
	// private String _multiPageKey = null;
	private String _mailCountKey = null;
	private String _characterSignatureKey = null;
	private String _syncFreqKey = null;
	private String _syncLedKey = null;
	private String _syncShakeKey = null;
	private String _syncSondKey = null;
//	private String _ringtoneKey = null;
	private String _serverAddressKey = null;
	private String _signature_textKey = null;
	private String _clear_historyKey = null;
	private String _clear_passwordKey = null;
	private String _clear_cacheKey = null;
	private String _key_mute_time = null;
    
//	CheckBoxPreference _multiPagePref;
	//ListPreference _mailCountPref;
	EditTextPreference _signaturePref;
	EditTextPreference _serverAddressPref;

	// public static String _serverAddText=null;

	//ListPreference _syncFreqPref;
	CheckBoxPreference _syncLedPref;
	CheckBoxPreference _syncShakePref;
	CheckBoxPreference _syncSondPref;
//	RingtonePreference _ringtonePref;
	Preference _clearhistoryPref;
	private CharSequence summaryHostoryId = null;
	private CharSequence summaryPossowordId = null;
	private CharSequence summaryCacheId = null;
	Preference _clearPassword;
	Preference _clearCache;
	private String newSignature;
	Preference _mutetimePref;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.getPreferenceManager()
				.setSharedPreferencesName(MyApp.SHARED_SETTING);
		// 所的的值将会自动保存到SharePreferences
		addPreferencesFromResource(R.xml.setting);

		Resources res = getResources();
		summaryHostoryId = res.getString(R.string.click_clear_hostoryId);
		summaryPossowordId = res.getString(R.string.click_clear_posswordId);
		summaryCacheId = res.getString(R.string.click_clear_cacheId);
		_mailCountKey = res.getString(R.string.key_mail_number);
		_syncFreqKey = res.getString(R.string.key_sync_frequency);
		_syncLedKey = res.getString(R.string.key_new_mail_led);
		_syncShakeKey = res.getString(R.string.key_new_mail_vibrate);
		_syncSondKey= res.getString(R.string.key_new_mail_sond);
		//_ringtoneKey = res.getString(R.string.key_new_mail_ringtone);
		_clear_historyKey = res.getString(R.string.key_clear_history);
		_clear_passwordKey = res.getString(R.string.key_clear_password);
		_clear_cacheKey = res.getString(R.string.key_clear_cache);

//		_mailCountPref = (ListPreference) findPreference(_mailCountKey);
//		_mailCountPref.setOnPreferenceChangeListener(this);
//		_mailCountPref.setSummary(_mailCountPref.getEntry());

		_characterSignatureKey = res.getString(R.string.key_mail_signature);
		_signaturePref = (EditTextPreference) findPreference(_characterSignatureKey);
		_signaturePref.setOnPreferenceChangeListener(this);
		_signature_textKey = MyApp.userSetting.getSignature();
		_signaturePref.setText(_signature_textKey);

		_key_mute_time=res.getString(R.string.key_mute_time);
		_mutetimePref=findPreference(_key_mute_time);
		_mutetimePref.setOnPreferenceChangeListener(this);
		
		_serverAddressKey = res.getString(R.string.key_server_address);
		_serverAddressPref = (EditTextPreference) findPreference(_serverAddressKey);
		if(_serverAddressPref != null)
		{//we have no server address preference in release build
			_serverAddressPref.setOnPreferenceChangeListener(this);
		}
		_clearPassword = findPreference(_clear_passwordKey);
		_clearCache = findPreference(_clear_cacheKey);

		_clearPassword.setSummary(summaryPossowordId);
		_clearCache.setSummary(summaryCacheId);

		_clearPassword
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						Builder dialog = new AlertDialog.Builder(
																	SettingPage.this)
								.setTitle(R.string.string_clear_password_title)
								.setMessage(R.string.stirng_confirm_clear_password)
								.setPositiveButton(	R.string.stirng_confirm_clear,
													new DialogInterface.OnClickListener() {

														@Override
														public void onClick(
																DialogInterface dialog,
																int which)
														{
															NewDbHelper
																	.getInstance()
																	.execSQL(	"update account set password=null",
																				new Object[] {});
														}
													})
								.setNegativeButton(	R.string.stirng_cancel_clear,
													new DialogInterface.OnClickListener() {

														@Override
														public void onClick(
																DialogInterface dialog,
																int which)
														{
															// TODO
															// Auto-generated
															// method stub
															return;
														}
													});
						dialog.show();
						return false;
					}

				});
		_clearCache
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						Builder dialog = new AlertDialog.Builder(
																	SettingPage.this)
								.setTitle(R.string.string_clear_cache_title)
								.setMessage(R.string.string_confirm_clear_cache)
								.setPositiveButton(	R.string.stirng_confirm_clear,
													new DialogInterface.OnClickListener() {

														@Override
														public void onClick(
																DialogInterface dialog,
																int which)
														{
															// NewDbHelper.getInstance()
															// .query("update mail set body=null;",
															// null);
															NewDbHelper
																	.getInstance()
																	.execSQL(	"update mail set body=null",
																				new Object[] {});
												            NewDbHelper
														            .getInstance()
														            .execSQL(   "delete from attachPreviewCache",
																                new Object[] {});
														}
													})
								.setNegativeButton(	R.string.stirng_cancel_clear,
													new DialogInterface.OnClickListener() {

														@Override
														public void onClick(
																DialogInterface dialog,
																int which)
														{
															// TODO
															// Auto-generated
															// method stub
															return;
														}
													});
						dialog.show();
						// NewDbHelper.getInstance().updateMailBody(inMailInfo);
						// NewDbHelper.getInstance()
						// .query("update mail set body=null;", null);
						return false;
					}
				});

		/*
		 * private void clearSearchHistory() { SearchRecentSuggestions
		 * suggestions =new SearchRecentSuggestions(this,
		 * SearchSuggestionSampleProvider.AUTHORITY,
		 * SearchSuggestionSampleProvider.MODE); suggestions.clearHistory(); }
		 */
		_clearhistoryPref = findPreference(_clear_historyKey);
		// _mailCountPref.setSummary(_mailCountPref.getEntry());
		_clearhistoryPref.setSummary(summaryHostoryId);
		_clearhistoryPref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						MailFolderActivity.clearSearchHistory();
						return false;
					}
				});

		// try
		// {
		// InetAddress addr = InetAddress.getLocalHost();
		// _serverAddress=addr.getHostAddress().toString();
		// }
		// catch (UnknownHostException e)
		// {
		// e.printStackTrace();
		// }

//		String countPerReception = _mailCountPref.getValue();
//		if (countPerReception == null)// first time
//		{
//			_mailCountPref.setValue(MyApp.userSetting.countPerReception + "");
////			_multiPagePref.setChecked(MyApp.userSetting.showMultipage);
//		}
//		else
//		{
//			MyApp.userSetting.countPerReception = Integer
//					.parseInt(countPerReception);
////			MyApp.userSetting.showMultipage = _multiPagePref.isChecked();
//		}
//		_mailCountPref.setSummary(_mailCountPref.getEntry());

//		_syncFreqPref = (ListPreference) findPreference(_syncFreqKey);
		_syncLedPref = (CheckBoxPreference) findPreference(_syncLedKey);
		_syncShakePref = (CheckBoxPreference) findPreference(_syncShakeKey);
		_syncSondPref=(CheckBoxPreference) findPreference(_syncSondKey);
//		_ringtonePref = (RingtonePreference) findPreference(_ringtoneKey);

//		_syncFreqPref.setOnPreferenceChangeListener(this);
		_syncLedPref.setOnPreferenceChangeListener(this);
		_syncShakePref.setOnPreferenceChangeListener(this);
		_syncSondPref.setOnPreferenceChangeListener(this);
//		_syncFreqPref.setSummary(_syncFreqPref.getEntry());
//		_ringtonePref.setOnPreferenceChangeListener(this);

		
//		String synFreq = _syncFreqPref.getValue();
		
//		if (synFreq == null)// first time
//		{
//			_syncFreqPref.setValue(MyApp.userSetting.getPushFrequency() + "");
			_syncLedPref.setChecked(MyApp.userSetting._ledFlag);
			_syncShakePref.setChecked(MyApp.userSetting._vibrateFlag);
			_syncSondPref.setChecked(MyApp.userSetting._sondFlag);
			// _ringtonePref.setDefaultValue(defaultValue)
//			if(MyApp.userSetting.getSignature().toString()!=""&&MyApp.userSetting.getSignature()!=null)
//			{
//				_signaturePref.setText(MyApp.userSetting.getSignature());	
//			}
			
			
//		}
//		else
//		{
//			int synFreqValue = Integer.parseInt(synFreq);
//			MyApp.userSetting.setPushFrequency(synFreqValue);
//			MyApp.userSetting._ledFlag = _syncLedPref.isChecked();
//			MyApp.userSetting._vibrateFlag = _syncShakePref.isChecked();
//			MyApp.userSetting._sondFlag=_syncSondPref.isChecked();
//			updateLedShakeEnableStatus(synFreqValue);
//		}

//		_syncFreqPref.setSummary(_syncFreqPref.getEntry());
		//_ringtonePref.setSummary(MyApp.userSetting._ringtone);
//		updateRingtoneSummary(_ringtonePref, Uri.parse(MyApp.userSetting._ringtone));

		// Log.d("multiplage mail",
		// MailClient.userSetting.showMultipage?"true":"false");
		// Log.d("mail count per reception", countPerReception);
	}

	// since when onPreferenceChanged invoked, the returned entry is the
	// previous changed value.
	String getCurrentEntry(ListPreference preference, String newValue)
	{
		CharSequence[] entries = preference.getEntries();
		CharSequence[] entryValues = preference.getEntryValues();
		for (int i = 0; i < entryValues.length; i++)
		{
			if (newValue.equals((String) entryValues[i]))
				return (String) entries[i];
		}
		return "";
	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		// TODO Auto-generated method stub
		Log.d(LOGTAG, "SystemSetting" + "preference is changed");
		// Log.d("Key_SystemSetting", preference.getKey());
		// 判断是哪个Preference改变了
		String keyName = preference.getKey();
		if (keyName.equals(_mailCountKey))
		{
//			String countPerReception = newValue.toString();// ((ListPreference)preference).getValue();
//			MyApp.userSetting.countPerReception = Integer
//					.parseInt(countPerReception);
//			_mailCountPref
//					.setSummary(getCurrentEntry((ListPreference) preference,
//												countPerReception));
//			Log.d(LOGTAG, "mail count per reception" + countPerReception);
		}
		else if (preference.getKey().equals(_characterSignatureKey))
		{
		    newSignature = newValue.toString();
			MyApp.userSetting.setSignatrue(newSignature);
//			_signaturePref.setText(newSignature);
			
			/*_signaturePref.setText(_signature_textKey);*/
			Log.d(LOGTAG, "Character signature" + newSignature);
//			return false;
		}
		else if (preference.getKey().equals(_serverAddressKey))
		{
			String newServerAdd = newValue.toString();
			// _serverAddText=newServerAdd;
			// _serverAddressPref.setText(_serverAddText);
			MyApp.userSetting._serverAddText = newServerAdd;
			Log.d(LOGTAG, "serverAddress" + newServerAdd);
		}
		else if (keyName.equals(_syncFreqKey))
		{
			int synFreqValue = Integer.parseInt(newValue.toString());
			MyApp.userSetting.setPushFrequency(synFreqValue);
//			_syncFreqPref
//					.setSummary(getCurrentEntry((ListPreference) preference,
//												newValue.toString()));
			// _syncFreqPref.setSummary(((ListPreference)
			// preference).getEntry());
		
			//stopService(MailFolderActivity._pushIntent);
			updateLedShakeEnableStatus(synFreqValue);
			MyApp.instance().setPollInterval(synFreqValue);
			Log.d(LOGTAG, "synchronous frequency" + synFreqValue + " minutes");
		}
		else if (keyName.equals(_syncLedKey))
		{
			boolean showLed = ((Boolean) newValue).booleanValue();
			MyApp.userSetting._ledFlag = showLed;
			Log.d(LOGTAG, "flick led" + (showLed ? "true" : "false"));
		}
		else if (keyName.equals(_syncShakeKey))
		{
			boolean shake = ((Boolean) newValue).booleanValue();
			MyApp.userSetting._vibrateFlag = shake;
			Log.d(LOGTAG, "shake phone" + (shake ? "true" : "false"));
		}
		else if (keyName.equals(_syncSondKey))
		{
			boolean sond = ((Boolean) newValue).booleanValue();
			MyApp.userSetting._sondFlag = sond;
			Log.d(LOGTAG, "sond phone" + (sond ? "true" : "false"));
		}
//		else if (keyName.equals(_ringtoneKey))
//		{
//			String ringtone = newValue.toString();
//			MyApp.userSetting._ringtone = ringtone;
//			updateRingtoneSummary((RingtonePreference) preference, Uri.parse((String) newValue));
//		}
		else if (keyName.equals(_key_mute_time))
		{
			String storeString = TimePickerPreference.storeString;
			MyApp.userSetting.muteTimeValue=storeString;
			_mutetimePref.setSummary(storeString);
		}
		
		else
		{
			return false;
		}
		return true;
	}

	

	private void updateLedShakeEnableStatus(int syncFreq)
	{
		boolean value = (syncFreq >= 0);
		_syncLedPref.setEnabled(value);
		_syncShakePref.setEnabled(value);
		_syncSondPref.setEnabled(value);
//		_ringtonePref.setEnabled(value);
	}
	
//	private void updateRingtoneSummary(RingtonePreference preference, Uri ringtoneUri){
//	    Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
//	    if (ringtone != null)
//	        preference.setSummary(ringtone.getTitle(this));
//	    else
//	        preference.setSummary(R.string.silent);
//	}
	
}
