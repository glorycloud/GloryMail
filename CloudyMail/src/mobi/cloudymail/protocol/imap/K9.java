
package mobi.cloudymail.protocol.imap;

import java.io.File;
import java.lang.reflect.Method;

import android.text.format.Time;
import android.util.Log;


public class K9 
{
    public static File tempDirectory;
    public static final String LOG_TAG = "CloudMail";

 
    public enum BACKGROUND_OPS
    {
        WHEN_CHECKED, ALWAYS, NEVER, WHEN_CHECKED_AUTO_SYNC
    }

    private static String language = "";
    private static int theme = android.R.style.Theme_Light;

 
    private static BACKGROUND_OPS backgroundOps = BACKGROUND_OPS.WHEN_CHECKED;
    /**
     * Some log messages can be sent to a file, so that the logs
     * can be read using unprivileged access (eg. Terminal Emulator)
     * on the phone, without adb.  Set to null to disable
     */
    public static final String logFile = null;
    //public static final String logFile = Environment.getExternalStorageDirectory() + "/k9mail/debug.log";

    /**
     * If this is enabled, various development settings will be enabled
     * It should NEVER be on for Market builds
     * Right now, it just governs strictmode
     **/
    public static boolean DEVELOPER_MODE = true;


    /**
     * If this is enabled there will be additional logging information sent to
     * Log.d, including protocol dumps.
     * Controlled by Preferences at run-time
     */
    public static boolean DEBUG = false;

    /**
     * Should K-9 log the conversation it has over the wire with
     * SMTP servers?
     */

    public static boolean DEBUG_PROTOCOL_SMTP = true;

    /**
     * Should K-9 log the conversation it has over the wire with
     * IMAP servers?
     */

    public static boolean DEBUG_PROTOCOL_IMAP = true;


    /**
     * Should K-9 log the conversation it has over the wire with
     * POP3 servers?
     */

    public static boolean DEBUG_PROTOCOL_POP3 = true;

    /**
     * Should K-9 log the conversation it has over the wire with
     * WebDAV servers?
     */

    public static boolean DEBUG_PROTOCOL_WEBDAV = true;



    /**
     * If this is enabled than logging that normally hides sensitive information
     * like passwords will show that information.
     */
    public static boolean DEBUG_SENSITIVE = false;

    /**
     * Can create messages containing stack traces that can be forwarded
     * to the development team.
     */
    public static boolean ENABLE_ERROR_FOLDER = true;
    public static String ERROR_FOLDER_NAME = "K9mail-errors";


    private static boolean mAnimations = true;

    private static boolean mConfirmDelete = false;
    private static boolean mKeyguardPrivacy = false;

    private static boolean mMessageListStars = true;
    private static boolean mMessageListCheckboxes = false;
    private static boolean mMessageListTouchable = false;
    private static int mMessageListPreviewLines = 2;

    private static boolean mShowCorrespondentNames = true;
    private static boolean mShowContactName = false;
    private static boolean mChangeContactNameColor = false;
    private static int mContactNameColor = 0xff00008f;
    private static boolean mMessageViewFixedWidthFont = false;
    private static boolean mMessageViewReturnToList = false;

    private static boolean mGesturesEnabled = true;
    private static boolean mUseVolumeKeysForNavigation = false;
    private static boolean mUseVolumeKeysForListNavigation = false;
    private static boolean mManageBack = false;
    private static boolean mStartIntegratedInbox = false;
    private static boolean mMeasureAccounts = true;
    private static boolean mCountSearchMessages = true;
    private static boolean mZoomControlsEnabled = false;
    private static boolean mMobileOptimizedLayout = false;
    private static boolean mQuietTimeEnabled = false;
    private static String mQuietTimeStarts = null;
    private static String mQuietTimeEnds = null;
    private static boolean compactLayouts = false;

    

    private static boolean useGalleryBugWorkaround = false;
    private static boolean galleryBuggy;


    /**
     * The MIME type(s) of attachments we're willing to view.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[]
    {
        "*/*",
    };

    /**
     * The MIME type(s) of attachments we're not willing to view.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_VIEW_TYPES = new String[]
    {
    };

    /**
     * The MIME type(s) of attachments we're willing to download to SD.
     */
    public static final String[] ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[]
    {
        "*/*",
    };

    /**
     * The MIME type(s) of attachments we're not willing to download to SD.
     */
    public static final String[] UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES = new String[]
    {
    };

    /**
     * The special name "INBOX" is used throughout the application to mean "Whatever folder
     * the server refers to as the user's Inbox. Placed here to ease use.
     */
    public static final String INBOX = "INBOX";

    /**
     * For use when displaying that no folder is selected
     */
    public static final String FOLDER_NONE = "-NONE-";

    public static final String LOCAL_UID_PREFIX = "K9LOCAL:";

    public static final String REMOTE_UID_PREFIX = "K9REMOTE:";

    public static final String IDENTITY_HEADER = "X-K9mail-Identity";

    /**
     * Specifies how many messages will be shown in a folder by default. This number is set
     * on each new folder and can be incremented with "Load more messages..." by the
     * VISIBLE_LIMIT_INCREMENT
     */
    public static int DEFAULT_VISIBLE_LIMIT = 25;

    /**
     * The maximum size of an attachment we're willing to download (either View or Save)
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB downloaded but only 5MB saved.
     */
    public static final int MAX_ATTACHMENT_DOWNLOAD_SIZE = (128 * 1024 * 1024);

    /**
     * Max time (in millis) the wake lock will be held for when background sync is happening
     */
    public static final int WAKE_LOCK_TIMEOUT = 600000;

    public static final int MANUAL_WAKE_LOCK_TIMEOUT = 120000;

    public static final int PUSH_WAKE_LOCK_TIMEOUT = 60000;

    public static final int MAIL_SERVICE_WAKE_LOCK_TIMEOUT = 30000;

    public static final int BOOT_RECEIVER_WAKE_LOCK_TIMEOUT = 60000;

    /**
     * Time the LED is on/off when blinking on new email notification
     */
    public static final int NOTIFICATION_LED_ON_TIME = 500;
    public static final int NOTIFICATION_LED_OFF_TIME = 2000;

    public static final boolean NOTIFICATION_LED_WHILE_SYNCING = false;
    public static final int NOTIFICATION_LED_FAST_ON_TIME = 100;
    public static final int NOTIFICATION_LED_FAST_OFF_TIME = 100;


    public static final int NOTIFICATION_LED_BLINK_SLOW = 0;
    public static final int NOTIFICATION_LED_BLINK_FAST = 1;



    public static final int NOTIFICATION_LED_SENDING_FAILURE_COLOR = 0xffff0000;

    // Must not conflict with an account number
    public static final int FETCHING_EMAIL_NOTIFICATION      = -5000;
    public static final int SEND_FAILED_NOTIFICATION      = -1500;
    public static final int CONNECTIVITY_ID = -3;


    public static class Intents
    {

        public static class EmailReceived
        {
            public static final String ACTION_EMAIL_RECEIVED    = "com.fsck.k9.intent.action.EMAIL_RECEIVED";
            public static final String ACTION_EMAIL_DELETED     = "com.fsck.k9.intent.action.EMAIL_DELETED";
            public static final String ACTION_REFRESH_OBSERVER  = "com.fsck.k9.intent.action.REFRESH_OBSERVER";
            public static final String EXTRA_ACCOUNT            = "com.fsck.k9.intent.extra.ACCOUNT";
            public static final String EXTRA_FOLDER             = "com.fsck.k9.intent.extra.FOLDER";
            public static final String EXTRA_SENT_DATE          = "com.fsck.k9.intent.extra.SENT_DATE";
            public static final String EXTRA_FROM               = "com.fsck.k9.intent.extra.FROM";
            public static final String EXTRA_TO                 = "com.fsck.k9.intent.extra.TO";
            public static final String EXTRA_CC                 = "com.fsck.k9.intent.extra.CC";
            public static final String EXTRA_BCC                = "com.fsck.k9.intent.extra.BCC";
            public static final String EXTRA_SUBJECT            = "com.fsck.k9.intent.extra.SUBJECT";
            public static final String EXTRA_FROM_SELF          = "com.fsck.k9.intent.extra.FROM_SELF";
        }

    }


    private void maybeSetupStrictMode()
    {
        if (!K9.DEVELOPER_MODE)
            return;

        try
        {
            Class<?> strictMode = Class.forName("android.os.StrictMode");
            Method enableDefaults = strictMode.getMethod("enableDefaults");
            enableDefaults.invoke(strictMode);
        }

        catch (Exception e)
        {
            // Discard , as it means we're not running on a device with strict mode
            Log.v(K9.LOG_TAG, "Failed to turn on strict mode "+e);
        }

    }




    public static String getK9Language()
    {
        return language;
    }

    public static void setK9Language(String nlanguage)
    {
        language = nlanguage;
    }

    public static int getK9Theme()
    {
        return theme;
    }

    public static void setK9Theme(int ntheme)
    {
        theme = ntheme;
    }

    public static BACKGROUND_OPS getBackgroundOps()
    {
        return backgroundOps;
    }

    public static boolean setBackgroundOps(BACKGROUND_OPS backgroundOps)
    {
        BACKGROUND_OPS oldBackgroundOps = K9.backgroundOps;
        K9.backgroundOps = backgroundOps;
        return backgroundOps != oldBackgroundOps;
    }

    public static boolean setBackgroundOps(String nbackgroundOps)
    {
        return setBackgroundOps(BACKGROUND_OPS.valueOf(nbackgroundOps));
    }

    public static boolean gesturesEnabled()
    {
        return mGesturesEnabled;
    }

    public static void setGesturesEnabled(boolean gestures)
    {
        mGesturesEnabled = gestures;
    }

    public static boolean useVolumeKeysForNavigationEnabled()
    {
        return mUseVolumeKeysForNavigation;
    }

    public static void setUseVolumeKeysForNavigation(boolean volume)
    {
        mUseVolumeKeysForNavigation = volume;
    }

    public static boolean useVolumeKeysForListNavigationEnabled()
    {
        return mUseVolumeKeysForListNavigation;
    }

    public static void setUseVolumeKeysForListNavigation(boolean enabled)
    {
        mUseVolumeKeysForListNavigation = enabled;
    }

    public static boolean manageBack()
    {
        return mManageBack;
    }

    public static void setManageBack(boolean manageBack)
    {
        mManageBack = manageBack;
    }

    public static boolean zoomControlsEnabled()
    {
        return mZoomControlsEnabled;
    }

    public static void setZoomControlsEnabled(boolean zoomControlsEnabled)
    {
        mZoomControlsEnabled = zoomControlsEnabled;
    }


    public static boolean mobileOptimizedLayout()
    {
        return mMobileOptimizedLayout;
    }

    public static void setMobileOptimizedLayout(boolean mobileOptimizedLayout)
    {
        mMobileOptimizedLayout = mobileOptimizedLayout;
    }

    public static boolean getQuietTimeEnabled()
    {
        return mQuietTimeEnabled;
    }

    public static void setQuietTimeEnabled(boolean quietTimeEnabled)
    {
        mQuietTimeEnabled = quietTimeEnabled;
    }

    public static String getQuietTimeStarts()
    {
        return mQuietTimeStarts;
    }

    public static void setQuietTimeStarts(String quietTimeStarts)
    {
        mQuietTimeStarts = quietTimeStarts;
    }

    public static String getQuietTimeEnds()
    {
        return mQuietTimeEnds;
    }

    public static void setQuietTimeEnds(String quietTimeEnds)
    {
        mQuietTimeEnds = quietTimeEnds;
    }


    public static boolean isQuietTime()
    {
        if (!mQuietTimeEnabled)
        {
            return false;
        }

        Time time = new Time();
        time.setToNow();
        Integer startHour = Integer.parseInt(mQuietTimeStarts.split(":")[0]);
        Integer startMinute = Integer.parseInt(mQuietTimeStarts.split(":")[1]);
        Integer endHour = Integer.parseInt(mQuietTimeEnds.split(":")[0]);
        Integer endMinute = Integer.parseInt(mQuietTimeEnds.split(":")[1]);

        Integer now = (time.hour * 60 ) + time.minute;
        Integer quietStarts = startHour * 60 + startMinute;
        Integer quietEnds =  endHour * 60 +endMinute;

        // If start and end times are the same, we're never quiet
        if (quietStarts.equals(quietEnds))
        {
            return false;
        }


        // 21:00 - 05:00 means we want to be quiet if it's after 9 or before 5
        if (quietStarts > quietEnds)
        {
            // if it's 22:00 or 03:00 but not 8:00
            if ( now >= quietStarts || now <= quietEnds)
            {
                return true;
            }
        }

        // 01:00 - 05:00
        else
        {

            // if it' 2:00 or 4:00 but not 8:00 or 0:00
            if ( now >= quietStarts && now <= quietEnds)
            {
                return true;
            }
        }

        return false;
    }



    public static boolean startIntegratedInbox()
    {
        return mStartIntegratedInbox;
    }

    public static void setStartIntegratedInbox(boolean startIntegratedInbox)
    {
        mStartIntegratedInbox = startIntegratedInbox;
    }

    public static boolean showAnimations()
    {
        return mAnimations;
    }

    public static void setAnimations(boolean animations)
    {
        mAnimations = animations;
    }

    public static boolean messageListTouchable()
    {
        return mMessageListTouchable;
    }

    public static void setMessageListTouchable(boolean touchy)
    {
        mMessageListTouchable = touchy;
    }

    public static int messageListPreviewLines()
    {
        return mMessageListPreviewLines;
    }

    public static void setMessageListPreviewLines(int lines)
    {
        mMessageListPreviewLines = lines;
    }

    public static boolean messageListStars()
    {
        return mMessageListStars;
    }

    public static void setMessageListStars(boolean stars)
    {
        mMessageListStars = stars;
    }
    public static boolean messageListCheckboxes()
    {
        return mMessageListCheckboxes;
    }

    public static void setMessageListCheckboxes(boolean checkboxes)
    {
        mMessageListCheckboxes = checkboxes;
    }

    public static boolean showCorrespondentNames()
    {
        return mShowCorrespondentNames;
    }

    public static void setShowCorrespondentNames(boolean showCorrespondentNames)
    {
        mShowCorrespondentNames = showCorrespondentNames;
    }

    public static boolean showContactName()
    {
        return mShowContactName;
    }

    public static void setShowContactName(boolean showContactName)
    {
        mShowContactName = showContactName;
    }

    public static boolean changeContactNameColor()
    {
        return mChangeContactNameColor;
    }

    public static void setChangeContactNameColor(boolean changeContactNameColor)
    {
        mChangeContactNameColor = changeContactNameColor;
    }

    public static int getContactNameColor()
    {
        return mContactNameColor;
    }

    public static void setContactNameColor(int contactNameColor)
    {
        mContactNameColor = contactNameColor;
    }

    public static boolean messageViewFixedWidthFont()
    {
        return mMessageViewFixedWidthFont;
    }

    public static void setMessageViewFixedWidthFont(boolean fixed)
    {
        mMessageViewFixedWidthFont = fixed;
    }

    public static boolean messageViewReturnToList()
    {
        return mMessageViewReturnToList;
    }

    public static void setMessageViewReturnToList(boolean messageViewReturnToList)
    {
        mMessageViewReturnToList = messageViewReturnToList;
    }

    public static Method getMethod(Class<?> classObject, String methodName)
    {
        try
        {
            return classObject.getMethod(methodName, boolean.class);
        }
        catch (NoSuchMethodException e)
        {
            Log.i(K9.LOG_TAG, "Can't get method " +
                  classObject.toString() + "." + methodName);
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Error while using reflection to get method " +
                  classObject.toString() + "." + methodName, e);
        }
        return null;
    }

    public static boolean measureAccounts()
    {
        return mMeasureAccounts;
    }

    public static void setMeasureAccounts(boolean measureAccounts)
    {
        mMeasureAccounts = measureAccounts;
    }

    public static boolean countSearchMessages()
    {
        return mCountSearchMessages;
    }

    public static void setCountSearchMessages(boolean countSearchMessages)
    {
        mCountSearchMessages = countSearchMessages;
    }

    public static boolean useGalleryBugWorkaround()
    {
        return useGalleryBugWorkaround;
    }

    public static void setUseGalleryBugWorkaround(boolean useGalleryBugWorkaround)
    {
        K9.useGalleryBugWorkaround = useGalleryBugWorkaround;
    }

    public static boolean isGalleryBuggy()
    {
        return galleryBuggy;
    }

    public static boolean confirmDelete()
    {
        return mConfirmDelete;
    }

    public static void setConfirmDelete(final boolean confirm)
    {
        mConfirmDelete = confirm;
    }

    /**
     * @return Whether privacy rules should be applied when system is locked
     */
    public static boolean keyguardPrivacy()
    {
        return mKeyguardPrivacy;
    }

    public static void setKeyguardPrivacy(final boolean state)
    {
        mKeyguardPrivacy = state;
    }
    
    public static boolean useCompactLayouts()
    {
        return compactLayouts;
    }

    public static void setCompactLayouts(boolean compactLayouts)
    {
        K9.compactLayouts = compactLayouts;
    }

 
}
