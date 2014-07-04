/**
 * Ace Shooting
 *
 * Copyright (c) 2014 Ace Shooting
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.aceshooting.rssapp;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.aceshooting.rssapp.provider.FeedData;

public class EntriesListActivity extends ListActivity implements Requeryable {
	private static final int CONTEXTMENU_MARKASREAD_ID = 6;
	
	private static final int CONTEXTMENU_MARKASUNREAD_ID = 7;
	
	private static final int CONTEXTMENU_DELETE_ID = 8;
	
	private static final int CONTEXTMENU_COPYURL = 9;
	
	public static final String EXTRA_SHOWFEEDINFO = "show_feedinfo";
	
	public static final String EXTRA_AUTORELOAD = "autoreload";
	
	private static final String FAVORITES = "favorites";
	
	private static final String ALLENTRIES = "allentries";
	
	private static final String[] FEED_PROJECTION = {FeedData.FeedColumns.NAME,
		FeedData.FeedColumns.URL,
		FeedData.FeedColumns.ICON,
		FeedData.FeedColumns.HIDE_READ
	};
	
	private Uri uri;
	
	private EntriesListAdapter entriesListAdapter;
	
	private byte[] iconBytes;
	
	private String feedName;
	
	private long feedId;
	
	private boolean hideRead;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (MainTabActivity.isLightTheme(this)) {
			setTheme(R.style.Theme_Light);
		}
		
		super.onCreate(savedInstanceState);
		
		feedName = null;
		
		iconBytes = null;
		
		Intent intent = getIntent();
		
		feedId = intent.getLongExtra(FeedData.FeedColumns._ID, 0);
		uri = intent.getData();
		
		if (feedId > 0) {
			Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI(feedId), FEED_PROJECTION, null, null, null);
			
			if (cursor.moveToFirst()) {
				feedName = cursor.isNull(0) ? cursor.getString(1) : cursor.getString(0);
				iconBytes = cursor.getBlob(2);
				hideRead = cursor.getInt(3) == 1;
			}
			cursor.close();
		} else {
			hideRead = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(new StringBuilder(uri.equals(FeedData.EntryColumns.FAVORITES_CONTENT_URI) ? FAVORITES : ALLENTRIES).append('.').append(FeedData.FeedColumns.HIDE_READ).toString(), false);
		}
		
		if (!MainTabActivity.POSTGINGERBREAD && iconBytes != null && iconBytes.length > 0) { // we cannot insert the icon here because it would be overwritten, but we have to reserve the icon here
			if (!requestWindowFeature(Window.FEATURE_LEFT_ICON)) {
				iconBytes = null;
			}
		}
		
		setContentView(R.layout.entries);
		
		entriesListAdapter = new EntriesListAdapter(this, uri, intent.getBooleanExtra(EXTRA_SHOWFEEDINFO, false), intent.getBooleanExtra(EXTRA_AUTORELOAD, false), hideRead);
		setListAdapter(entriesListAdapter);
		
		if (feedName != null) {
			setTitle(feedName);
		}
		if (iconBytes != null && iconBytes.length > 0) {
			int bitmapSizeInDip = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, getResources().getDisplayMetrics());
			Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
			if (bitmap != null) {
				if (bitmap.getHeight() != bitmapSizeInDip) {
					bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSizeInDip, bitmapSizeInDip, false);
				}
				
				if (MainTabActivity.POSTGINGERBREAD) {
					CompatibilityHelper.setActionBarDrawable(this, new BitmapDrawable(bitmap));
				} else {
					setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(bitmap));
				}
			}
		}
		if (RSSOverview.notificationManager != null) {
			RSSOverview.notificationManager.cancel(0);
		}
		
		getListView().setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(((TextView) ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView.findViewById(android.R.id.text1)).getText());
				menu.add(0, CONTEXTMENU_MARKASREAD_ID, Menu.NONE, R.string.contextmenu_markasread).setIcon(android.R.drawable.ic_menu_manage);
				menu.add(0, CONTEXTMENU_MARKASUNREAD_ID, Menu.NONE, R.string.contextmenu_markasunread).setIcon(android.R.drawable.ic_menu_manage);
				//menu.add(0, CONTEXTMENU_DELETE_ID, Menu.NONE, R.string.contextmenu_delete).setIcon(android.R.drawable.ic_menu_delete);
				menu.add(0, CONTEXTMENU_COPYURL, Menu.NONE, R.string.contextmenu_copyurl).setIcon(android.R.drawable.ic_menu_share);
			}
		});
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, long id) {
		TextView textView = (TextView) view.findViewById(android.R.id.text1);
		
		textView.setTypeface(Typeface.DEFAULT);
		textView.setEnabled(false);
		view.findViewById(android.R.id.text2).setEnabled(false);
		entriesListAdapter.neutralizeReadState();
		startActivity(new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(uri, id)).putExtra(FeedData.FeedColumns.HIDE_READ, entriesListAdapter.isHideRead()).putExtra(FeedData.FeedColumns.ICON, iconBytes).putExtra(FeedData.FeedColumns.NAME, feedName));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.entrylist, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (menu != null) {
			menu.setGroupVisible(R.id.menu_group_0, entriesListAdapter.getCount() > 0);
			/*if (hideRead) {
				menu.findItem(R.id.menu_hideread).setChecked(true).setTitle(R.string.contextmenu_showread).setIcon(android.R.drawable.ic_menu_view);
			} else {
				menu.findItem(R.id.menu_hideread).setChecked(false).setTitle(R.string.contextmenu_hideread).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
			}*/
			
		}
		return true;
	}
	
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_markasread: {
				new Thread() { // the update process takes some time
					public void run() {
						getContentResolver().update(uri, RSSOverview.getReadContentValues(), null, null);
					}
				}.start();
				entriesListAdapter.markAsRead();
				break;
			}
			case R.id.menu_markasunread: {
				new Thread() { // the update process takes some time
					public void run() {
						getContentResolver().update(uri, RSSOverview.getUnreadContentValues(), null, null);
					}
				}.start();
				entriesListAdapter.markAsUnread();
				break;
			}
			/*case R.id.menu_hideread: {
				hideRead = !entriesListAdapter.isHideRead();
				if (hideRead) {
					item.setChecked(false).setTitle(R.string.contextmenu_hideread).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
					entriesListAdapter.setHideRead(true);
				} else {
					item.setChecked(true).setTitle(R.string.contextmenu_showread).setIcon(android.R.drawable.ic_menu_view);
					entriesListAdapter.setHideRead(false);
				}
				setHideReadFromUri();
				break;
			}
			case R.id.menu_deleteread: {
				new Thread() { // the delete process takes some time
					public void run() {
						String selection = Strings.READDATE_GREATERZERO+Strings.DB_AND+" ("+Strings.DB_EXCUDEFAVORITE+")";
						
						getContentResolver().delete(uri, selection, null);
						FeedData.deletePicturesOfFeed(EntriesListActivity.this, uri, selection);
						runOnUiThread(new Runnable() {
							public void run() {
								entriesListAdapter.getCursor().requery();
							}
						});
					}
				}.start();
				break;
			}
			case R.id.menu_deleteallentries: {
				Builder builder = new AlertDialog.Builder(this);
				
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setTitle(R.string.contextmenu_deleteallentries);
				builder.setMessage(R.string.question_areyousure);
				builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						new Thread() {
							public void run() {
								getContentResolver().delete(uri, Strings.DB_EXCUDEFAVORITE, null);
								runOnUiThread(new Runnable() {
									public void run() {
										entriesListAdapter.getCursor().requery();
									}
								});
							}
						}.start();
					}
				});
				builder.setNegativeButton(android.R.string.no, null);
				builder.show();
				break;
			}
			case CONTEXTMENU_MARKASREAD_ID: {
				long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;
				
				getContentResolver().update(ContentUris.withAppendedId(uri, id), RSSOverview.getReadContentValues(), null, null);
				entriesListAdapter.markAsRead(id);
				break;
			}
			case CONTEXTMENU_MARKASUNREAD_ID: {
				long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;
				
				getContentResolver().update(ContentUris.withAppendedId(uri, id), RSSOverview.getUnreadContentValues(), null, null);
				entriesListAdapter.markAsUnread(id);
				break;
			}
			case CONTEXTMENU_DELETE_ID: {
				long id = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id;
				
				getContentResolver().delete(ContentUris.withAppendedId(uri, id), null, null);
				FeedData.deletePicturesOfEntry(Long.toString(id));
				entriesListAdapter.getCursor().requery(); // we have no other choice
				break;
			}*/
			case CONTEXTMENU_COPYURL: {
				((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setText(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).targetView.getTag().toString());
				break;
			}
			
		}
		return true;
	}
	
	private void setHideReadFromUri() {
		if (feedId > 0) {
			ContentValues values = new ContentValues();
			
			values.put(FeedData.FeedColumns.HIDE_READ, hideRead);
			getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(feedId), values, null, null);
		} else {
			Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
			
			editor.putBoolean(new StringBuilder(uri.equals(FeedData.EntryColumns.FAVORITES_CONTENT_URI) ? FAVORITES : ALLENTRIES).append('.').append(FeedData.FeedColumns.HIDE_READ).toString(), hideRead);
			editor.commit();
		}
	}

	@Override
	public void requery() {
		if (entriesListAdapter != null) {
			entriesListAdapter.reloadCursor();
		}
	}
}

