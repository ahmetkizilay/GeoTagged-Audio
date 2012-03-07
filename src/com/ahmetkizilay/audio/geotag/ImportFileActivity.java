package com.ahmetkizilay.audio.geotag;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

// TODO use icons for folder and zip files
public class ImportFileActivity extends Activity{
	
	private String selectedPath;
	private ListView listView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.import_main);

		try {
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				throw new Exception("External SD Card Is Not Detected!");
			}

			this.selectedPath = Environment.getExternalStorageDirectory().getAbsolutePath();

			String action = getIntent().getAction();
			
			if (action != null && action.equals("com.ahmetkizilay.audio.geotag.IMPORT_FILE")) {
				prepareListView();
				updateListView();
			}
			else {
				Toast.makeText(ImportFileActivity.this, "Unrecognized Action", Toast.LENGTH_LONG).show();
			}

		} catch (Exception exp) {
			Toast.makeText(ImportFileActivity.this, "Error: " + exp.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void prepareListView() {
		listView = (ListView) findViewById(R.id.lwImport);

	}

	private void updateListView() {
		List<String> fileItemList = new ArrayList<String>();

		if (!this.selectedPath.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
			fileItemList.add("..");
		}

		File parentFolder = new File(selectedPath);
		File[] itemsToBeListed = parentFolder.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				return pathname.isDirectory() || pathname.getName().endsWith(".zip");
			}

		});

		for (int i = 0; i < itemsToBeListed.length; i++) {
			fileItemList.add(itemsToBeListed[i].getName());
		}

		//listView.removeAllViews();
		listView.setAdapter(new ArrayAdapter<String>(this, R.layout.import_list, fileItemList));
		listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {

				TextView selectedTextView = (TextView) view;
				String selectedText = selectedTextView.getText().toString();
				
				if(selectedText.equals("..")) {
					int lastIndex = selectedPath.lastIndexOf('/');
					selectedPath = selectedPath.substring(0, lastIndex);
				} else if(selectedText.endsWith(".zip")) {
					handleFileChoose(selectedPath + "/" + selectedTextView.getText().toString());
					return;
				}
				else {
					selectedPath += "/" + selectedTextView.getText();					
				}


				updateListView();
			}
		});
		
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
				TextView selectedTextView = (TextView) view;
				String selectedText = selectedTextView.getText().toString();
				
				if(selectedText.equals("..")) {
					// nothingtemp = "back back";
				}else {					
					handleFileChoose(selectedPath + "/" + selectedTextView.getText().toString());					
				}
				
				// Toast.makeText(ImportExportFileActivity.this, selectedPath, Toast.LENGTH_SHORT).show();
				return true;
			}
			
		});
		listView.invalidate();
	}
	
	private void handleFileChoose(String chosenFile) {
		if(chosenFile.endsWith(".zip")) {
			Intent data = new Intent();
			data.putExtra("selectedFile", chosenFile);
			setResult(Activity.RESULT_OK, data);
			finish();
		}
	}
}
