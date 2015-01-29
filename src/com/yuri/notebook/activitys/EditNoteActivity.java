package com.yuri.notebook.activitys;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.frontia.FrontiaData;
import com.baidu.frontia.FrontiaQuery;
import com.yuri.notebook.NoteManager;
import com.yuri.notebook.R;
import com.yuri.notebook.R.array;
import com.yuri.notebook.R.color;
import com.yuri.notebook.R.drawable;
import com.yuri.notebook.R.id;
import com.yuri.notebook.R.layout;
import com.yuri.notebook.R.string;
import com.yuri.notebook.bean.Note;
import com.yuri.notebook.db.MetaData;
import com.yuri.notebook.db.MetaData.NoteColumns;
import com.yuri.notebook.net.FrontiaManager;
import com.yuri.notebook.utils.LogUtils;
import com.yuri.notebook.utils.NoteUtil;

public class EditNoteActivity extends Activity implements OnItemSelectedListener {
	private static final String TAG = EditNoteActivity.class.getSimpleName();
	private EditText mContentEdit;

	private long mTime;
	private String mTitle = "";
	private String mContent = "";
	private String mGroup = "";
	
	long itemId = -1;
	
	private Note mNoteBook;
	
	private SharedPreferences sp = null;
	
	private Spinner mSpinner;
	
	private String[] mGroups = null;
	
	FrontiaManager mFrontiaManager = null;
	
	public static final int MSG_UPLOAD_OVER = 0x01;
	public static final int MSG_UPDATE_OVER = 0x02;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPLOAD_OVER:
				int ret = msg.arg1;
				if (ret == 0) {
					Toast.makeText(getApplicationContext(), "上传成功", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getApplicationContext(), "上传失败", Toast.LENGTH_SHORT).show();
				}
				
				break;
			case MSG_UPDATE_OVER:
				int ret1 = msg.arg1;
				if (ret1 == 0) {
					Toast.makeText(getApplicationContext(), "更新成功", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getApplicationContext(), "更新失败", Toast.LENGTH_SHORT).show();
				}
				break;

			default:
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.add_note);
		
		ActionBar actionBar = getActionBar();
		actionBar.setIcon(R.drawable.ic_title_bar_done);
		actionBar.setHomeButtonEnabled(true);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View customView = inflater.inflate(R.layout.editor_custom_actionbar, null);
		View saveMenuItem = customView.findViewById(R.id.save_menu_item);
		saveMenuItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doSaveAction();
			}
		});
		View cancelMenuItem = customView.findViewById(R.id.cancel_menu_item);
		cancelMenuItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				EditNoteActivity.this.finish();
			}
		});
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, 
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
				ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customView);
		mContentEdit = (EditText)findViewById(R.id.content_edit);
		mContentEdit.setBackgroundColor(Color.TRANSPARENT);
		mContentEdit.requestFocus();
		
		mSpinner = (Spinner) findViewById(R.id.spinner);
		MySpinnerAdapter adapter = new MySpinnerAdapter();
		mSpinner.setAdapter(adapter);
		mSpinner.setOnItemSelectedListener(this);
		
		mGroups = getResources().getStringArray(R.array.group_list);
		
		Intent intent = this.getIntent();
		itemId = intent.getLongExtra(NoteUtil.ITEM_ID_INDEX, -1);
		if (itemId == -1) {
			mContentEdit.setText("");
			mNoteBook = new Note();
			mNoteBook.init();
		} else {
			mNoteBook = NoteManager.getNotesFromId(itemId,this);
			mContentEdit.setText(mNoteBook.getContent());
			mContentEdit.setSelection(mNoteBook.getContent().length());
			
			int position = 0;
			for (int i = 0; i < mGroups.length; i++) {
				if (mGroups[i].equals(mNoteBook.getGroup())) {
					position = i;
				}
			}
			mSpinner.setSelection(position);
		}
		
		mFrontiaManager = new FrontiaManager();
	}
	
	private void doSaveAction(){
		String content = mContentEdit.getText().toString().trim();
		if (itemId == -1) {
			if (content.equals("")) {
				Toast.makeText(this, R.string.empty_note_1, Toast.LENGTH_SHORT).show();
				finish();
				return;
			} 
			
			String objectId = Note.getObjectId(System.currentTimeMillis());
			mNoteBook.setObjectId(objectId);
			
			ContentValues values = new ContentValues();
			values.put(NoteColumns.OBJECT_ID, objectId);
			values.put(NoteColumns.CONTENT, content);
			values.put(NoteColumns.GROUP, mGroup);
			values.put(NoteColumns.TIME, System.currentTimeMillis());
			getContentResolver().insert(NoteColumns.CONTENT_URI, values);
			
			//upload to cloud
			mNoteBook.setContent(content);
			mNoteBook.setTime(System.currentTimeMillis());
			mNoteBook.setGroup(mGroup);
			mFrontiaManager.insertData(mNoteBook, mHandler);
			
			setResult(RESULT_OK);
			Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
			finish();
		}else {
			if (content.equals("")) {
				Toast.makeText(this, R.string.empty_note, Toast.LENGTH_SHORT).show();
				return;
			} else {
				if (!content.equals(mNoteBook.getContent()) || !mGroup.equals(mNoteBook.getGroup())) {
					updateNote(content, null, mGroup);
					setResult(RESULT_OK);
					Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
				}
				finish();
			}
		}
	}
	
	private void updateNote(String content, String title, String group){
		mTime = System.currentTimeMillis();
		ContentValues values = new ContentValues();
		values.put(MetaData.NoteColumns.CONTENT, content);
		values.put(MetaData.NoteColumns.TIME, mTime);
		values.put(NoteColumns.GROUP, mGroup);
		
		Uri uri = Uri.parse(MetaData.NoteColumns.CONTENT_URI + "/" + itemId);
		getContentResolver().update(uri, values, null, null);
		
		LogUtils.d(TAG, "updateNote.objectId:" + mNoteBook.getObjectId());
		FrontiaQuery frontiaQuery = new FrontiaQuery();
		frontiaQuery.equals(Note.FRONTIA_KEY, Note.FRONTIA_VALUE);
		frontiaQuery.equals(Note.OBJECTID, mNoteBook.getObjectId());
		
		FrontiaData frontiaData = new FrontiaData();
		frontiaData.put(Note.OBJECTID, mNoteBook.getObjectId());
		frontiaData.put(Note.CONTENT, content);
		frontiaData.put(Note.GROUP, group);
		frontiaData.put(Note.TIME, mTime);
		mFrontiaManager.update(frontiaQuery, frontiaData, mHandler);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem add = menu.add(0, NoteUtil.MENU_SAVE, 0, R.string.menu_save).setIcon(R.drawable.ic_select_all); 
        add.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM); 

//		return super.onCreateOptionsMenu(menu);
        return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case NoteUtil.MENU_SAVE:
			mTime = System.currentTimeMillis();
			
			if (mTitle.equals("")) {
				NoteUtil.showToast(EditNoteActivity.this, R.string.title_cannot_null);
			}else {
				mContent = mContentEdit.getText().toString().trim();
				ContentValues values = new ContentValues();
				values.put(MetaData.NoteColumns.CONTENT, mContent);
				values.put(MetaData.NoteColumns.TIME, mTime);
				values.put(NoteColumns.GROUP, mGroup);
				
				Uri uri = Uri.parse(MetaData.NoteColumns.CONTENT_URI + "/" + itemId);
				getContentResolver().update(uri, values, null, null);
				
				Intent intent = new Intent();
				intent.putExtra("time", mTime);
				setResult(RESULT_OK,intent);
				EditNoteActivity.this.finish();
			}

			break;
		case android.R.id.home://
			//点击左上角应用图标，返回，默认情况下，图标的ID是android.R.id.home
			noteFinish();
			
			break;
		}
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode) {
			doSaveAction();
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void noteFinish(){
		mContent = mContentEdit.getText().toString().trim();
		if (mNoteBook.getContent().equals(mContent)) {
			this.finish();
		}else {
			NoteManager.backAlert(EditNoteActivity.this);
		}
	}
	
	class MySpinnerAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 5;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
//			ColorStateList textColor = getResources().getColorStateList(R.color.)
			LinearLayout layout = new LinearLayout(EditNoteActivity.this);
			layout.setOrientation(LinearLayout.HORIZONTAL);
			TextView tvColor = new TextView(EditNoteActivity.this);
			tvColor.setPadding(0, 5, 0, 0);
			tvColor.setHeight((int)(getResources().getDisplayMetrics().density * 48 + 0.5f));
			tvColor.setWidth((int)(getResources().getDisplayMetrics().density * 6 + 0.5f));
			switch (position) {
			case 0:
				tvColor.setBackgroundResource(R.color.none);
				break;
			case 1:
				tvColor.setBackgroundResource(R.color.work);
				break;
			case 2:
				tvColor.setBackgroundResource(R.color.personal);
				break;
			case 3:
				tvColor.setBackgroundResource(R.color.family);
				break;
			case 4:
				tvColor.setBackgroundResource(R.color.study);
				break;
			default:
				break;
			}
			layout.addView(tvColor);
			
			TextView tvGroup = new TextView(EditNoteActivity.this);
			tvGroup.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
			tvGroup.setText(mGroups[position]);
			tvGroup.setTextSize(16);
			tvGroup.setPadding(0, 
							   0, 
							   (int)(getResources().getDisplayMetrics().density * 9 + 0.5f), 
							   0);
			tvGroup.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			layout.addView(tvGroup);
			return layout;
		}
		
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		mGroup = mGroups[position];
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
		
	}
}
