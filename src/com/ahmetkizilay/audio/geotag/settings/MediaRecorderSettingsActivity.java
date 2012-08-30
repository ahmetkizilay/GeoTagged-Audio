package com.ahmetkizilay.audio.geotag.settings;

import java.util.ArrayList;
import java.util.List;

import com.ahmetkizilay.audio.geotag.R;


import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

public class MediaRecorderSettingsActivity extends Activity{

	private Spinner spOutputType;
	private Spinner spAudioEncoder;
	private Spinner spSamplingRate;
	private Spinner spBitRate;
	
	private Button btnSave;
	private Button btnCancel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.audio_settings);
		
		Intent callerIntent = getIntent();
		int selectedOutputType = Integer.parseInt(callerIntent.getExtras().get("selectedOutputType").toString());
		int selectedSamplingRate = Integer.parseInt(callerIntent.getExtras().get("selectedSamplingRate").toString());
		int selectedBitRate = Integer.parseInt(callerIntent.getExtras().get("selectedBitRate").toString());
		int selectedAudioEncoder = Integer.parseInt(callerIntent.getExtras().get("selectedAudioEncoder").toString());
		
		spOutputType = (Spinner) findViewById(R.id.spOutputType_as);
		final List<SpinnerItem> spitOutputType = new ArrayList<SpinnerItem>();
		spitOutputType.add(new SpinnerItem("Not Specified", -1));
		spitOutputType.add(new SpinnerItem("3GP", MediaRecorder.OutputFormat.THREE_GPP));
		//spitOutputType.add(new SpinnerItem("MPEG-4", MediaRecorder.OutputFormat.MPEG_4));
		final CustomSpinnerAdapter csaOutputType = new CustomSpinnerAdapter(this, spitOutputType);
		spOutputType.setAdapter(csaOutputType);
		spOutputType.setSelection(csaOutputType.getItemPosition(selectedOutputType));
		
		spAudioEncoder = (Spinner) findViewById(R.id.spAudioEncoder_as);
		final List<SpinnerItem> spitAudioEncoder = new ArrayList<SpinnerItem>();
		spitAudioEncoder.add(new SpinnerItem("Not Specified", -1));
		spitAudioEncoder.add(new SpinnerItem("AAC", MediaRecorder.AudioEncoder.AAC));
		spitAudioEncoder.add(new SpinnerItem("AMR_NB", MediaRecorder.AudioEncoder.AMR_NB));
		spitAudioEncoder.add(new SpinnerItem("AMR_WB", MediaRecorder.AudioEncoder.AMR_WB));
		final CustomSpinnerAdapter csaAudioEncoder = new CustomSpinnerAdapter(this, spitAudioEncoder);
		spAudioEncoder.setAdapter(csaAudioEncoder);
		spAudioEncoder.setSelection(csaAudioEncoder.getItemPosition(selectedAudioEncoder));
		
		spSamplingRate = (Spinner) findViewById(R.id.spSamplingRate_as);
		final List<SpinnerItem> spitSamplingRate = new ArrayList<SpinnerItem>();
		spitSamplingRate.add(new SpinnerItem("Not Specified", -1));
		spitSamplingRate.add(new SpinnerItem("8000", 8000));
		spitSamplingRate.add(new SpinnerItem("16000", 16000));
		spitSamplingRate.add(new SpinnerItem("22050", 22050));
		spitSamplingRate.add(new SpinnerItem("44100", 44100));
		spitSamplingRate.add(new SpinnerItem("48000", 48000));
		final CustomSpinnerAdapter csaSamplingRate = new CustomSpinnerAdapter(this, spitSamplingRate);
		spSamplingRate.setAdapter(csaSamplingRate);
		spSamplingRate.setSelection(csaSamplingRate.getItemPosition(selectedSamplingRate));
		
		spBitRate = (Spinner) findViewById(R.id.spBitRate_as);
		final List<SpinnerItem> spitBitRate = new ArrayList<SpinnerItem>();
		spitBitRate.add(new SpinnerItem("Not Specified", -1));
		spitBitRate.add(new SpinnerItem("8", 8));
		spitBitRate.add(new SpinnerItem("16", 16));
		final CustomSpinnerAdapter csaBitRate = new CustomSpinnerAdapter(this, spitBitRate);
		spBitRate.setAdapter(csaBitRate);
		spBitRate.setSelection(csaBitRate.getItemPosition(selectedBitRate));
		
		spAudioEncoder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parentView, View selectedView, int position, long id) {
				
				SpinnerItem item = (SpinnerItem) parentView.getAdapter().getItem(position);
				if(item.getValue() == MediaRecorder.AudioEncoder.AMR_NB) {
					spSamplingRate.setSelection(1);
					spSamplingRate.setEnabled(false);
				}
				else if(item.getValue() == MediaRecorder.AudioEncoder.AMR_WB) {
					spSamplingRate.setSelection(2);
					spSamplingRate.setEnabled(false);
				}
				else {
					spSamplingRate.setEnabled(true);
				}
				
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
	
		});
		
		btnSave = (Button) findViewById(R.id.btnSave_as);
		btnSave.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				HandleSaveButtonClicked();
			}
		});
		
		btnCancel = (Button) findViewById(R.id.btnCancel_as);
		btnCancel.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				HandleCancelButtonClicked();
			}
		});
	}
	
	private void HandleSaveButtonClicked() {
		int selectedOutputType = ((SpinnerItem) spOutputType.getSelectedItem()).getValue();
		int selectedAudioEncoder = ((SpinnerItem) spAudioEncoder.getSelectedItem()).getValue();
		int selectedSamplingRate = ((SpinnerItem) spSamplingRate.getSelectedItem()).getValue();
		int selectedBitRate = ((SpinnerItem) spBitRate.getSelectedItem()).getValue();
		
		Intent data = new Intent();
		data.putExtra("selectedOutputType", selectedOutputType);
		data.putExtra("selectedAudioEncoder", selectedAudioEncoder);
		data.putExtra("selectedSamplingRate", selectedSamplingRate);
		data.putExtra("selectedBitRate", selectedBitRate);
		
		setResult(Activity.RESULT_OK, data);
		finish();
	}
	
	private void HandleCancelButtonClicked() {
		setResult(Activity.RESULT_CANCELED, new Intent());
		finish();
	}
}
