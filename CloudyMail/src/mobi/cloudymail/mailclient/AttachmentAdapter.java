package mobi.cloudymail.mailclient;

import java.util.List;

import mobi.cloudymail.mailclient.net.AttachmentInfo;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class AttachmentAdapter extends BaseAdapter{
    private Context context;
    private LayoutInflater layoutInflater;
    private List<AttachmentInfo> attachments;
    private Composer _composerActivity;
    
	public AttachmentAdapter(Context context, 
			List<AttachmentInfo> attachmenet) {
		_composerActivity=((Composer)context);
		this.context = context;
		this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.attachments = attachmenet;
	}

	@Override
	public int getCount() {
		return attachments.size();
	}

	@Override
	public Object getItem(int position) {
		return attachments.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view=convertView;
		if(view == null)
			view = layoutInflater.inflate(R.layout.attachment_item_layout, null);
		ImageView attachIconView=(ImageView) view.findViewById(R.id.attachImage);
		TextView attachNameView=(TextView)view.findViewById(R.id.attachName);
		ImageButton delAttachBtn=(ImageButton)view.findViewById(R.id.delAttachItem);
		
		delAttachBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_composerActivity.delAttachment(position);
			}
		});
		AttachmentInfo attachmentInfo = attachments.get(position);
		attachNameView.setText(attachmentInfo.fileName+"("+attachmentInfo.size+")");
		return view;
	}

}
