package mobi.cloudymail.mailclient;

import java.util.Vector;

import mobi.cloudymail.data.InMailInfo;
import mobi.cloudymail.data.MailInfo;
import mobi.cloudymail.data.MailStatus;
import mobi.cloudymail.mailclient.MailFolderActivity.ViewHolder;
import mobi.cloudymail.util.MyApp;
import mobi.cloudymail.util.Utils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

 class MailGroupAdapter extends BaseExpandableListAdapter 
	{
//		private MailInfo[] _mails;
	  
//		private MailFolderActivity _ctx;
//		private Vector<MailGroup> groupList;
//		private List<MailInfo> childList;
//		private MailGroup[] _groups;
	    private Vector<MailGroup> _groupVec;
		private MailFolderActivity _ctx;
		private OnClickListener hasMoreGroupListener;
//		private Map<MailGroup,MailInfo> _Group=null;

		public MailGroupAdapter(Vector<MailGroup> groupVec,MailFolderActivity _ctx) {
			this._ctx = _ctx;
			_groupVec=groupVec;
		}
		public void setGroups(Vector<MailGroup> mailGroup)
		{
			_groupVec=mailGroup;
			notifyDataSetChanged();
		}


		@Override
		public int getGroupCount() {
			if(!_ctx.inGroupMode())
			{
				return _ctx.getTotalMailCount(false);
			}
			return _groupVec.size();
//			return 0;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			int size = _ctx.getGroup(groupPosition).getGroupSize();
			if(size <= 1)
				return 0;
			return size;
		}
		@Override
		public int getGroupType(int groupPosition)
		{
			if(_ctx.inGroupMode())
			{
				MailGroup group = _ctx.getGroup(groupPosition);
				if(group.getGroupSize() > 1)
					return 0;
				if(group.getGroupSize()==1 && (group.getState()&MailStatus.FLAG_HAS_MORE_PLACEHOLD) != 0)
				{
					return 2;
				}
				return 1;
			}
			else
			{
				MailInfo info = _ctx.getMail(groupPosition);
				if((info.getState()&MailStatus.FLAG_HAS_MORE_PLACEHOLD) != 0)
					return 2;
				else
					return 1;
			}
		}
		@Override
		public Object getGroup(int groupPosition){
			return _ctx.getGroup(groupPosition);
//			return _ctx.getMail(groupPosition);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
//			return _mailList.get(groupPosition).get(childPosition);
			return _ctx.getGroup(groupPosition).getMailInfo(childPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}
		@Override
		public int getGroupTypeCount()
		{
			return 3;
		}
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
		{
			if(!_ctx.inGroupMode())
			{
				return getMailView(_ctx.getMail(groupPosition), convertView);
			}
			MailGroup group = _ctx.getGroup(groupPosition);
			
//			if ((group.getStatu() & MailStatus.FLAG_HAS_MORE_PLACEHOLD) != 0)
//			{// this is a place holder to indicate there are more mails
//				
////				return getHasMoreView(convertView, group);
//				return getMailView(group.getMailInfo(0), convertView);
//
//			}
		
			Utils.ASSERT(group.getGroupSize()>0);
		    if(group.getGroupSize()==1)
			{
		    	convertView = getMailView(group.getMailInfo(0), convertView);
				return convertView;
			}
			
			if(convertView==null)
				convertView = _ctx.getLayoutInflater().inflate(R.layout.mail_group_item, null);	
			TextView groupDate = (TextView)convertView.findViewById(R.id.groupDateTxt);
			TextView groupFrom = (TextView)convertView.findViewById(R.id.groupFromTxt);
			TextView groupSubject = (TextView)convertView.findViewById(R.id.groupSubjectTxt);
			ImageView groupImage = (ImageView) convertView.findViewById(R.id.mailGroupImage);
			TextView expandMarkView=(TextView)convertView.findViewById(R.id.expand_mark_group);
			if(group!=null)
			{
				String subject = group.getSuffix();
				groupSubject.setText(subject);
				groupFrom.setText(group.getFrom());
				if(group.getDate() != null)
					groupDate.setText(Utils.nearFormat.format(group.getDate()));
				else
					Utils.ASSERT(false);
			}
			groupImage.setImageResource(R.drawable.expander_ic_minimized);
//			expandMarkView.setHeight(convertView.getHeight());
			expandMarkView.setVisibility(View.GONE);
			if(isExpanded)
			{
				expandMarkView.setVisibility(View.VISIBLE);
				groupImage.setImageResource(R.drawable.expander_ic_maximized);
			}
				
			if (group.isReaded())
			{
				convertView.setBackgroundResource(R.drawable.mail_read_bg);
			}
			else
				convertView.setBackgroundResource(R.drawable.mail_unread_bg);

			return convertView;
		}
		private View getMailView(MailInfo mailInfo, View convertView) {
			//ExpandableListView elv = _ctx.getExpandableListView();
			if ((mailInfo.getState() & MailStatus.FLAG_HAS_MORE_PLACEHOLD) != 0)
			{// this is a place holder to indicate there are more mails
				
				if (convertView == null)
				{
					convertView = _ctx.getLayoutInflater()
							.inflate(R.layout.showmore_item, null);
				}
				if(_ctx instanceof InBoxActivity && ((InBoxActivity)_ctx).isItemInReceivingState(mailInfo.getUid()))
				{
					convertView.findViewById(R.id.showmore_progressBar).setVisibility(View.VISIBLE);
					convertView.findViewById(R.id.showmore_text).setVisibility(View.GONE);
				}
				else
				{
					convertView.findViewById(R.id.showmore_progressBar).setVisibility(View.GONE);
					convertView.findViewById(R.id.showmore_text).setVisibility(View.VISIBLE);
				}
				return convertView;
			}
			
			// else, this is a normal mail

			// A ViewHolder keeps references to children views to avoid
			// unnecessary calls
			// to findViewById() on each row.
			final ViewHolder holder;
			if (convertView == null)
			{
				convertView = _ctx.getLayoutInflater()
						.inflate(R.layout.mail_item, null);
//				TextView expandMarkView_child=(TextView)convertView.findViewById(R.id.expand_mark_child);
//				expandMarkView_child.setVisibility(View.GONE);
				// Creates a ViewHolder and store references to the two children
				// views
				// we want to bind data to.
				holder = new ViewHolder();
				
				holder.starBtn = (Button) convertView
						.findViewById(R.id.starTargetBtn);
				holder.newMailBtn=(Button)convertView.findViewById(R.id.newMailBtn);
				holder.starBtn.setOnClickListener(_ctx);
				holder.mailDelBtn = (Button) convertView
						.findViewById(R.id.delMail);
				holder.calendarFlagView = convertView.findViewById(R.id.calendarFlag);
				holder.normalAttachmentFlagView = convertView.findViewById(R.id.attachmentFlag);
				holder.replyFlagView=convertView.findViewById(R.id.replyFlag);
				holder.forwardFlagView=convertView.findViewById(R.id.forwardFlag);
				holder.dateTxt = (TextView) convertView
						.findViewById(R.id.dateTxt);
				holder.fromTxt = (TextView) convertView
						.findViewById(R.id.fromTxt);
				holder.draftTxt=(TextView)convertView.findViewById(R.id.draftTxt);
				holder.subjectTxt = (TextView) convertView
						.findViewById(R.id.subjectTxt);
				holder.mailItemCtx = (CheckBox) convertView
						.findViewById(R.id.mailItemCtx);
				holder.mailItemCtx.setOnCheckedChangeListener(_ctx);
				holder.groupFlag=(TextView)convertView.findViewById(R.id.expand_mark_child);
				convertView.setPadding(0, 2, 0, 2);
				convertView.setTag(holder);

				holder.mailDelBtn.setWidth(40);
				holder.mailDelBtn.setHeight(40);
				holder.mailDelBtn.setOnClickListener(_ctx);
				
			}
			else
			{
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
				holder.newMailBtn.setVisibility(View.GONE);
			}

			if(mailInfo instanceof InMailInfo)
			{
				if(MyApp.instance().containsMailInMap(mailInfo))
				{
					holder.newMailBtn.setVisibility(View.VISIBLE);
				}
				else {
					holder.newMailBtn.setVisibility(View.GONE);
				}
				
			}
			
			holder.dateTxt.setText(mailInfo.getDateString());
			holder.fromTxt.setText(new EmailAddress(mailInfo.getFrom())
					.getName());
			holder.subjectTxt.setText(mailInfo.getSubject());
			holder.mailItemCtx.setTag(mailInfo);
			holder.mailItemCtx.setChecked(_ctx._checkedMails.isSelected(mailInfo));
            holder.calendarFlagView.setVisibility(mailInfo.hasCalendarAttachment()?View.VISIBLE:View.GONE);
            holder.normalAttachmentFlagView.setVisibility(mailInfo.hasNormalAttachment()?View.VISIBLE:View.GONE);
            holder.replyFlagView.setVisibility(mailInfo.hasReply()?View.VISIBLE:View.INVISIBLE);
            holder.forwardFlagView.setVisibility(mailInfo.hasForward()?View.VISIBLE:View.INVISIBLE);
			int asteriskValue = mailInfo.getAsterisk();
			//init AsteriskBtnBackGround
			if(asteriskValue==1)
			{
				holder.starBtn.setBackgroundResource(R.drawable.btn_star_big_buttonless_on);
			}
			else
			{
				holder.starBtn.setBackgroundResource(R.drawable.btn_star_big_buttonless_off);
			}
			holder.starBtn.setTag(mailInfo);

			if (mailInfo.getState() != MailStatus.MAIL_READED)
			{
				convertView.setBackgroundResource(R.drawable.mail_unread_bg);
			}
			else
				convertView.setBackgroundResource(R.drawable.mail_read_bg);

//			if(position == 0)
//			{
//				convertView.setPadding(2,10, 2, 2);
//			}
			if (showGestureDelButton(mailInfo))
			{
				holder.mailDelBtn.setVisibility(View.VISIBLE);
	            holder.mailDelBtn.setTag(mailInfo);
			}
			else
			{
				holder.mailDelBtn.setVisibility(View.GONE);
			}
			_ctx.updateMailItemView(mailInfo, holder, convertView); //fix bug 395
			return convertView;
		}
		private boolean showGestureDelButton(MailInfo info)
		{
			if(_ctx.gesturePosition < 0)
				return false;
			if(ExpandableListView.getPackedPositionType(_ctx.gesturePosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
			{
				int groupPos = ExpandableListView.getPackedPositionGroup(_ctx.gesturePosition);
				int childPos = ExpandableListView.getPackedPositionChild(_ctx.gesturePosition);
				MailGroup g = _ctx.getGroup(groupPos);
				MailInfo info2 = g.getMailInfo(childPos);
				return info2 != null && info2.equals(info);
			}
			else if(ExpandableListView.getPackedPositionType(_ctx.gesturePosition) == ExpandableListView.PACKED_POSITION_TYPE_GROUP)
			{
				int groupPos = ExpandableListView.getPackedPositionGroup(_ctx.gesturePosition);
				MailGroup g = _ctx.getGroup(groupPos);
				return (g.getGroupSize() == 1 && info.equals(g.getMailInfo(0)));
			}
			return false;
		}

        

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			MailInfo mailInfo = _ctx.getGroup(groupPosition).getMailInfo(childPosition);
			
			View view=getMailView(mailInfo, convertView);
			TextView expandMarkView=(TextView)view.findViewById(R.id.expand_mark_child);
			expandMarkView.setVisibility(View.VISIBLE);
//			expandMarkView.setHeight(view.getHeight());
			
			return view;
		}
			
		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}