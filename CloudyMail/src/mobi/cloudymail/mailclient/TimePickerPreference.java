package mobi.cloudymail.mailclient;
import mobi.cloudymail.util.MyApp;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TimePicker;

public class TimePickerPreference extends Preference 
{
	private Context context=this.getContext();
    private TimePickerDialog startTimePickerDialog=null;
    private TimePickerDialog endTimePickerDialog=null;
    private CheckBox interruptCheBox;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;
    private String interruptTime;
	private boolean muteEnabled;
    public static int INTERRUPT_TIME_SLOT;
    public static int START_INTERRUPT_TIME;
    public static boolean IS_MUTE_FLAG=false;
    public static  String storeString=null;
    public static boolean checkBoxStatue;
	private OnCheckedChangeListener checkBoxListener;
	private OnClickListener startTimeBtnOnClickListener;
	private OnClickListener endTimeSetBtnOnClickListener;
	private OnDismissListener startTimeDismissListener;
	private OnDismissListener endTimeDismissListenere;
	private OnClickListener viewOnClickListener;
	private View view;
	private CharSequence summary; 
    
	public TimePickerPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setPersistent(true);
		initTimePickerDialog();
	}
	@Override
    protected void onBindView(View view) {
        super.onBindView(view);
        this.view = view;
//      view.inflate(context, R.layout.custom_timepicker_preference, null);
        interruptCheBox=(CheckBox) view.findViewById(R.id.timePickerCheckBox);
        interruptTime="6:00-7:00-false";
        interruptTime=MyApp.userSetting.getMuteTimeValue();
		String[] timeValue = interruptTime.split("-");
		
		String[] startTime = timeValue[0].split(":");
		startHour = Integer.parseInt(startTime[0]);
		startMinute = Integer.parseInt(startTime[1]);
		
		String[] endTime = timeValue[1].split(":");
		endHour = Integer.parseInt(endTime[0]);
		endMinute =Integer.parseInt(endTime[1]);
		
		muteEnabled = Boolean.parseBoolean(timeValue[2]);
//		setSummary(interruptTime);
	}
	
	
    /**
     * Init timePickerDialog
     */
	private void initTimePickerDialog()
	{
		
		    TimePickerDialog.OnTimeSetListener startOnTimeSetList=new TimePickerDialog.OnTimeSetListener(){  
	            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	            	startHour=hourOfDay;
	            	startMinute=minute;
	            }   
	        };  
	        
	        TimePickerDialog.OnTimeSetListener endOnTimeSetList=new TimePickerDialog.OnTimeSetListener(){  
	        	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	        		endHour=hourOfDay;
	        		endMinute=minute;
	        	}   
	        };  
	        
	        startTimePickerDialog=new TimePickerDialog(context, startOnTimeSetList, startHour, startMinute, true);
            endTimePickerDialog=new TimePickerDialog(context,endOnTimeSetList,endHour,endMinute,true);
	}
	/* (non-Javadoc)
	 * @see mobi.cloudymail.mailclient.startTimeSetListener#getView(android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View  getView(View convertView, ViewGroup parent)
	{
		View v = super.getView(convertView, parent);
		if(v == convertView)
			return v;
		TextView startMuteText=(TextView) v.findViewById(R.id.muteStartText);
		String startTempString=String.valueOf(startMinute);
		String endTempString=String.valueOf(endMinute);
		if(startTempString.length()==1)
		{
			startMuteText.setText(startHour+":"+"0"+startMinute);
		}
		else
		{
			startMuteText.setText(startHour+":"+startMinute);	
		}
		
		TextView endMuteText=(TextView) v.findViewById(R.id.muteEndText);
		if(endTempString.length()==1)
		{
			endMuteText.setText(endHour+":"+"0"+endMinute);
		}
		else
		{
			endMuteText.setText(endHour+":"+endMinute);
		}
		
	    interruptCheBox = (CheckBox) v.findViewById(R.id.timePickerCheckBox);
	    if(muteEnabled)
		{
			interruptCheBox.setChecked(true);
		}
		else
		{
			interruptCheBox.setChecked(false);
		}
	    /**
	     * view listener
	     */
	    
	    if(viewOnClickListener==null)
	    viewOnClickListener=new OnClickListener(){

			@Override
			public void onClick(View v)
			{
				muteEnabled=!muteEnabled;
				interruptCheBox.setChecked(muteEnabled);
			}};
	    v.setOnClickListener(viewOnClickListener);
	    
	    /**
	     * muteCheckBox listener
	     */
		if(checkBoxListener == null)
			checkBoxListener = new OnCheckedChangeListener()
			{
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					// Set muteEnabled
					muteEnabled = interruptCheBox.isChecked();
					MyApp.userSetting.muteEnabled = muteEnabled;
					storeString = startHour + ":" + startMinute + "-" + endHour + ":" + endMinute + "-" + muteEnabled;
					MyApp.userSetting.setMuteTimeValue(storeString);
					persistString(storeString);
					callChangeListener(storeString);
				}
				
			};
		interruptCheBox.setOnCheckedChangeListener(checkBoxListener);
	    /**
	     * startTimeBtn listerer
	     */
	    Button startTimeSetBtn=(Button) v.findViewById(R.id.startTimePickerBtn);
	    if(startTimeBtnOnClickListener==null)
	    startTimeBtnOnClickListener=new View.OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
                startTimePickerDialog.updateTime(startHour, startMinute);
                startTimePickerDialog.show();
               
			}
		};
	    startTimeSetBtn.setOnClickListener(startTimeBtnOnClickListener);
	    
	    /**
	     * endTimeBtn listener
	     */
	    Button endTimeSetBtn=(Button) v.findViewById(R.id.endTimePickerBtn);
	    
	    if(endTimeSetBtnOnClickListener==null)
	    endTimeSetBtnOnClickListener=new View.OnClickListener() {
	    	@Override
	    	public void onClick(View v)
	    	{
	    		endTimePickerDialog.updateTime(endHour, endMinute);
	    		endTimePickerDialog.show();
	    	
	    	}
	    };
	    endTimeSetBtn.setOnClickListener(endTimeSetBtnOnClickListener);
	    /**
	     *  startDismiss listener
	     */
	    if(startTimeDismissListener==null)
	    startTimeDismissListener=new OnDismissListener() {
		@Override
		public void onDismiss(DialogInterface dialog)
		{
	        startTimePickerDialog.onSaveInstanceState();
	        endTimePickerDialog.onSaveInstanceState();
	        //Preference values  save time
	        storeString = startHour+":"+startMinute+"-"+endHour+":"+endMinute+"-"+muteEnabled;
			
			persistString(storeString);
			callChangeListener(storeString);
			
		}
	    };
	  
	  startTimePickerDialog.setOnDismissListener(startTimeDismissListener);
	  
	  /**
	   * endDismiss listener
	   */
	  if(endTimeDismissListenere==null)
	  endTimeDismissListenere=new OnDismissListener() {
		@Override
		public void onDismiss(DialogInterface dialog)
		{
			storeString = startHour+":"+startMinute+"-"+endHour+":"+endMinute+"-"+muteEnabled;
			persistString(storeString);
			callChangeListener(storeString);
			
		}
	  };	    
    	endTimePickerDialog.setOnDismissListener(endTimeDismissListenere);    
	    return v;
  }
	@Override
	public void  setSummary(CharSequence summary)
	{
        if (summary == null && this.summary != null || summary != null && !summary.equals(this.summary)) {
        	this.summary  = summary;
    		super.setSummary(null);
    		notifyChanged();
        }
	}


}
  
