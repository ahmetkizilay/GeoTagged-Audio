package com.ahmetkizilay.audio.geotag;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.ahmetkizilay.audio.geotag.settings.MediaRecorderSettings;
import com.ahmetkizilay.audio.geotag.settings.MediaRecorderSettingsActivity;
import com.ahmetkizilay.audio.geotag.upload.SCUploadActivity;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class GeoTaggedAudioActivity extends MapActivity {

	private static final int OPEN_FILE_DIALOG = 2;
	private static final int LOCATION_SETTINGS_DIALOG = 3;
	private static final int ABOUT_PAGE_DIALOG = 4;
	private static final int ERROR_DIALOG = 5;
	private static final int EXPORT_FILE_DIALOG = 6;
	private static final int SAVE_FILE_DIALOG = 10;
	private static final int MEDIA_RECORDER_SETTINGS_INTENT_RESULT = 11;
	private static final int IMPORT_INTENT_RESULT = 12;	
	private static final int UPLOAD_INTENT_RESULT = 13;
	private static final String MEDIA_RECORDER_SETTINGS_FILE_NAME = "audrecset_file";
	
	private String home_directory_string;
	private String tempSoundFile;
	private String tempLocationFile;

	private String selectedSoundFile;
	private String selectedLocationFile;

	private String tempLocationBuffer;

	private MediaPlayer mediaPlayer;
	private MediaRecorder mediaRecorder;
	
	private MediaRecorderSettings mediaRecorderSettings;

	private long recordStartTime;
	
	private LocationManager locationManager;
	private LocationListener locationListener;
	
	List<CoordinateInfo> coordinates;
	
	private MapView mapView;
	private MapController mapController;
	private MarkerOverlay markerOverlay;
	private RouteOverlay routeOverlay;

	private Button btnPlay;
	private Button btnStop;
	private Button btnRec;
	private Button btnOpen;

	private SeekBar seekBar;

	private GeoPoint currentPoint = new GeoPoint(41036446, 28984707);

	private boolean isPlaying = false;
	private boolean isRecording = false;
	
	private PowerManager.WakeLock wakeLock;

	private String errorMessage = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		try {
			
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				throw new Exception("External SD Card Is Not Detected!");
			}

			home_directory_string = Environment.getExternalStorageDirectory().getAbsolutePath() + "/perisonic";
			File home_directory = new File(home_directory_string);
			if (!home_directory.exists() && !home_directory.mkdirs()) {
				throw new Exception("home directory could not be created!");
			}

			home_directory_string += "/audio";
			home_directory = new File(home_directory_string);
			if (!home_directory.exists() && !home_directory.mkdirs()) {
				throw new Exception("home directory could not be created!");
			}

			mapView = (MapView) findViewById(R.id.mapView);
			mapController = mapView.getController();
			mapController.setCenter(currentPoint);
			mapController.setZoom(12);


			this.markerOverlay = new MarkerOverlay(this.getResources().getDrawable(R.drawable.pin_small));
			//mapView.getOverlays().add(this.markerOverlay);

			btnPlay = (Button) findViewById(R.id.btnPlay);
			btnPlay.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					btnRec.setEnabled(false);
					btnPlay.setEnabled(false);
					btnStop.setEnabled(true);
					btnOpen.setEnabled(false);

					btnPlayOnClick();
				}
			});

			btnStop = (Button) findViewById(R.id.btnStop);
			btnStop.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					if (isPlaying)
						stopPlaying();
					if (isRecording)
						stopRecording();
				}
			});

			btnRec = (Button) findViewById(R.id.btnRec);
			btnRec.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					btnRec.setEnabled(false);
					btnPlay.setEnabled(false);
					btnStop.setEnabled(true);
					btnOpen.setEnabled(false);

					btnRecordOnClick();
				}
			});

			btnOpen = (Button) findViewById(R.id.btnOpen);
			btnOpen.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {
					showDialog(OPEN_FILE_DIALOG);
				}
			});
			
			btnStop.setEnabled(false);
			btnPlay.setEnabled(false);

			seekBar = (SeekBar) findViewById(R.id.seekBar1);
			seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				public void onStopTrackingTouch(SeekBar seekBar) {
					if (isPlaying && mediaPlayer != null && mediaPlayer.isPlaying()) {
						mediaPlayer.seekTo(seekBar.getProgress());
					}
				}

				public void onStartTrackingTouch(SeekBar seekBar) {

				}

				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

				}
			});


			setupMediaRecorderSettings();
			setupLocationServices();
			
			// acquireWakeLock();
			
		    // Thread.setDefaultUncaughtExceptionHandler(new CustomUncaughtExceptionHandler());

		} catch (Exception ex) {
			errorMessage = "Create: " + ex.getMessage();
			showDialog(ERROR_DIALOG);
		}
	}
	

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		if(isPlaying) {
			stopPlaying();
		}
		if(isRecording) {
			stopRecording();
		}
		
		locationManager.removeUpdates(locationListener);	
		
		releaseWakeLock();
	}

	private void stopRecording() {
		isRecording = false;
		saveFile(tempLocationFile, tempLocationBuffer);
		showDialog(SAVE_FILE_DIALOG);
		
		releaseWakeLock();
		
//		prepareRouteOverlay();

		btnRec.setEnabled(true);
		btnStop.setEnabled(false);
		btnPlay.setEnabled(true);
		btnOpen.setEnabled(true);

		if (mediaRecorder != null) {
			mediaRecorder.stop();
			mediaRecorder.release();
			mediaRecorder = null;
		}
	}

	private void btnPlayOnClick() {

		try {
			
			acquireWakeLock();
			
			// String currentSoundFile =
			// Environment.getExternalStorageDirectory().getAbsolutePath() +
			// "/perisonic/audio/istiklal.ogg";
			if (selectedSoundFile == null || selectedSoundFile.equals("")) {
				Toast.makeText(this, "No File Selected Yet", Toast.LENGTH_SHORT);
				return;
			}
			isPlaying = true;

			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setDataSource(selectedSoundFile);

			mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
				public void onPrepared(MediaPlayer arg0) {
					mediaPlayerOnPrepared();
				}
			});

			mediaPlayer.prepare();
			mediaPlayer.start();
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer mp) {
					stopPlaying();
				}
			});

		} catch (Exception exp) {
			errorMessage = "Play Click: " + exp.getMessage();
			showDialog(ERROR_DIALOG);
		} 
	}

	private void mediaPlayerOnPrepared() {
		seekBar.setMax(mediaPlayer.getDuration());
		Thread t = new Thread(new Runnable() {
			public void run() {

				int errorCount = 0;
				// List<CoordinateInfo> coordinates =
				// CoordinateInfo.createCoordinateInfoListFromFile(Environment.getExternalStorageDirectory().getAbsolutePath()
				// + "/perisonic/audio/test.txt");
				
				prepareRouteOverlay();

				while (isPlaying) {
					try {
						Thread.sleep(1000);
						int currentPosition = mediaPlayer.getCurrentPosition();

						seekBar.setProgress(currentPosition);

						currentPoint = getCurrentPoint(coordinates, currentPosition);
						// mapController.setCenter(currentPoint);

						//mapView.getOverlays().remove(markerOverlay);
						markerOverlay.clearOverlays();
						OverlayItem overlayItem = new OverlayItem(currentPoint, "You are here", "You are here");
						markerOverlay.addOverlay(overlayItem);
						if(!mapView.getOverlays().contains(markerOverlay))
							mapView.getOverlays().add(markerOverlay);
						
						mapView.postInvalidate();
					} catch (Exception exp) {
						errorCount++;
						if (errorCount > 100) {
							isPlaying = false;
							Toast.makeText(GeoTaggedAudioActivity.this, "Çok Hata", Toast.LENGTH_LONG);
						}
					}

				}
			}			
		});

		t.start();
	}

	private void btnRecordOnClick() {
		try {

			if (routeOverlay != null) {
				mapView.getOverlays().remove(routeOverlay);
				routeOverlay.clearCoordinates();
				routeOverlay = null;
				
				mapView.invalidate();
			}
			
			acquireWakeLock();
			
			long currentTime = System.currentTimeMillis();
			tempSoundFile = home_directory_string + "/temp" + currentTime + ".3gp";
			tempLocationFile = home_directory_string + "/temp" + currentTime + ".txt";
			if (currentPoint != null) {
				tempLocationBuffer = currentPoint.getLatitudeE6() + " " + currentPoint.getLongitudeE6() + " 0\n";
			} else {
				Toast.makeText(GeoTaggedAudioActivity.this, "Location Is Not Set Yet", Toast.LENGTH_SHORT);
				return;
			}
			mediaRecorder = new MediaRecorder();
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mediaRecorder.setOutputFile(tempSoundFile);
			mediaRecorder.setMaxDuration(0);
			
			if(mediaRecorderSettings.isOutputTypeSet()) {
				mediaRecorder.setOutputFormat(mediaRecorderSettings.getOutputType());
			}
			else {
				mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			}
			
			if(mediaRecorderSettings.isAudioEncoderSet()) {
				mediaRecorder.setAudioEncoder(mediaRecorderSettings.getAudioEncoder());
			}
			else {
				mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
			}
			
			if(mediaRecorderSettings.isSamplingRateSet()) {
				mediaRecorder.setAudioSamplingRate(mediaRecorderSettings.getSamplinRate());
			}
			else {
				mediaRecorder.setAudioSamplingRate(44100);
			}
			
			if(mediaRecorderSettings.isBitRateSet()) {
				mediaRecorder.setAudioEncodingBitRate(mediaRecorderSettings.getBitRate());
			}
			else {
				mediaRecorder.setAudioEncodingBitRate(16);
			}

			mediaRecorder.prepare();
			mediaRecorder.start();

			recordStartTime = System.currentTimeMillis();
			isRecording = true;

		} catch (Exception exp) {
			isRecording = false;
			errorMessage = "Error with startRecording " + exp.getMessage();
			showDialog(ERROR_DIALOG);
		}

	}

	private void stopPlaying() {
		isPlaying = false;

		releaseWakeLock();
		
		btnRec.setEnabled(true);
		btnPlay.setEnabled(true);
		btnStop.setEnabled(false);
		btnOpen.setEnabled(true);

		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.reset();
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	// calculating the current point on the map...
	// this will require further optimization
	private GeoPoint getCurrentPoint(List<CoordinateInfo> coordinates, long currentTime) {

		for (int i = 0; i < coordinates.size(); i++) {
			CoordinateInfo thisCoordinateInfo = coordinates.get(i);
			if (currentTime < thisCoordinateInfo.getTime()) {

				double currentRatio = calculateRatio(coordinates, i, currentTime);
				int currentLatitude = calculateLatitude(coordinates, i, currentRatio);
				int currentLongitude = calculateLongitude(coordinates, i, currentRatio);

				return new GeoPoint(currentLatitude, currentLongitude);
			}
		}

		CoordinateInfo lastCoordinateInfo = coordinates.get(coordinates.size() - 1);
		return new GeoPoint(lastCoordinateInfo.getLatitude(), lastCoordinateInfo.getLongitude());
	}

	private int calculateLatitude(List<CoordinateInfo> coordinates, int index, double ratio) {
		int latdif = coordinates.get(index + 1).getLatitude() - coordinates.get(index).getLatitude();
		int newlat = coordinates.get(index + 1).getLatitude() + (int) (latdif * ratio);
		return newlat;
	}

	private int calculateLongitude(List<CoordinateInfo> coordinates, int index, double ratio) {
		int longDif = coordinates.get(index + 1).getLongitude() - coordinates.get(index).getLongitude();
		int newLong = coordinates.get(index + 1).getLongitude() + (int) (longDif * ratio);
		return newLong;
	}

	private double calculateRatio(List<CoordinateInfo> coordinates, int index, long currentTime) {
		long timeDif = coordinates.get(index + 1).getTime() - coordinates.get(index).getTime();
		long timePassed = currentTime - coordinates.get(index).getTime();
		double ratio = (double) timePassed / (double) timeDif;
		return ratio;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case SAVE_FILE_DIALOG:
			return createSaveFileDialog();
		case OPEN_FILE_DIALOG:
			return createOpenFileDialog();
		case LOCATION_SETTINGS_DIALOG:
			return createLocationSettingsDialog();
		case ABOUT_PAGE_DIALOG:
			return createAboutDialog();
		case ERROR_DIALOG:
			return createErrorDialog();
		case EXPORT_FILE_DIALOG: 
			return createExportFileDialog();
		default:
			return null;
		}

	}
	
	private Dialog createErrorDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Error").setMessage(errorMessage).setCancelable(false).setNeutralButton("OK", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
			}
		});
		return builder.create();
	}

	private Dialog createLocationSettingsDialog() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setMessage("Please Enable A Location Provider").setCancelable(false).setPositiveButton("Go To Settings", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(callGPSSettingIntent);
			}
		});
		alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		return alertDialogBuilder.create();
	}

	private Dialog createExportFileDialog() {

		File homeDirectory = new File(home_directory_string);
		final String[] fileNames = homeDirectory.list(new FilenameFilter() {

			public boolean accept(File dir, String filename) {
				if (filename.endsWith(".3gp")) {
					String textFileName = filename.replace(".3gp", ".txt");
					if (new File(home_directory_string + "/" + textFileName).exists()) {
						return true;
					}
				}
				return false;
			}
		});

		for (int i = 0; i < fileNames.length; i++) {
			fileNames[i] = fileNames[i].replace(".3gp", "");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose File To Export");
		
		builder.setOnCancelListener(new OnCancelListener() {			
			public void onCancel(DialogInterface dialog) {
				removeDialog(EXPORT_FILE_DIALOG);
			}
		});
		builder.setItems(fileNames, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				String zipLocation = zipSelectedItem(fileNames[item]);
				if(zipLocation != null) {
					emailZipFile(zipLocation);
				}
			}
		});
		return builder.create();
	}
	
	private void emailZipFile(String zipLocation) {		
		String selectedFile = zipLocation.replace("/mnt/", "/");
		
		Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("application/zip");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Sending Love");
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hope you like this");		
		emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + selectedFile)); 
		startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	}
	
	private String zipSelectedItem(String fileName) {
		
		ZipOutputStream zop = null;
		BufferedInputStream gp3Bis = null;
		BufferedInputStream txtBis = null;
		try {
	
			byte[] buffer = new byte[1024]; 
			int bytes_read;
			
			String zippedFilePath = (home_directory_string + "/" + fileName + ".zip");
			String gp3File = (home_directory_string + "/" + fileName + ".3gp");
			String txtFile = (home_directory_string + "/" + fileName + ".txt");
			
			
			zop = new ZipOutputStream(new FileOutputStream(new File(zippedFilePath)));
			
			FileInputStream gp3Fis = new FileInputStream(new File(gp3File));
			gp3Bis = new BufferedInputStream(gp3Fis, 1024);
			ZipEntry gp3entry = new ZipEntry(fileName + ".3gp");
			zop.putNextEntry(gp3entry);
			
			while((bytes_read = gp3Bis.read(buffer, 0, 1024)) != -1) {
				zop.write(buffer, 0, bytes_read);
			}
			
			gp3Bis.close();
			
			FileInputStream txtFis = new FileInputStream(new File(txtFile));
			txtBis = new BufferedInputStream(txtFis, 1024);
			ZipEntry txtentry = new ZipEntry(fileName + ".txt");
			zop.putNextEntry(txtentry);
			
			while((bytes_read = txtBis.read(buffer, 0, 1024)) != -1) {
				zop.write(buffer, 0, bytes_read);
			}
			
			txtBis.close();
			
			zop.flush();
			zop.close();
			
			return zippedFilePath;
			
		}
		catch(Exception exp) {
			Toast.makeText(GeoTaggedAudioActivity.this, "Error Zipping Files", Toast.LENGTH_LONG);
			if(gp3Bis != null) try { gp3Bis.close(); } catch(Exception ex) {}
			if(txtBis != null) try { txtBis.close(); } catch(Exception ex) {}
			if(zop != null) try { zop.close(); } catch(Exception ex) {}
			return null;
		}
	}
	
	private Dialog createOpenFileDialog() {

		File homeDirectory = new File(home_directory_string);
		final String[] fileNames = homeDirectory.list(new FilenameFilter() {

			public boolean accept(File dir, String filename) {
				if (filename.endsWith(".3gp")) {
					String textFileName = filename.replace(".3gp", ".txt");
					if (new File(home_directory_string + "/" + textFileName).exists()) {
						return true;
					}
				}
				return false;
			}
		});

		for (int i = 0; i < fileNames.length; i++) {
			fileNames[i] = fileNames[i].replace(".3gp", "");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose A File");
		
		builder.setOnCancelListener(new OnCancelListener() {			
			public void onCancel(DialogInterface dialog) {
				removeDialog(OPEN_FILE_DIALOG);
			}
		});
		builder.setItems(fileNames, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				Toast.makeText(getApplicationContext(), fileNames[item], Toast.LENGTH_SHORT).show();
				selectedSoundFile = home_directory_string + "/" + fileNames[item] + ".3gp";
				selectedLocationFile = home_directory_string + "/" + fileNames[item] + ".txt";
				
				prepareRouteOverlay();
				
				btnPlay.setEnabled(true);
				removeDialog(OPEN_FILE_DIALOG);
			}
		});
		return builder.create();
	}

	private Dialog createSaveFileDialog() {
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
		final AlertDialog alert = new AlertDialog.Builder(GeoTaggedAudioActivity.this).create();
		// alert.setCancelable(false);
		alert.setTitle("set file name");
		alert.setView(textEntryView);
		alert.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				EditText editText = (EditText) alert.findViewById(R.id.new_file_name);
				String enteredText = editText.getText().toString().trim();
				if(enteredText.equals("")) {
					enteredText = "new" + System.currentTimeMillis();
				}
				selectedSoundFile = home_directory_string + "/" + enteredText + ".3gp";
				selectedLocationFile = home_directory_string + "/" + enteredText + ".txt";
				new File(tempSoundFile).renameTo(new File(selectedSoundFile));
				new File(tempLocationFile).renameTo(new File(selectedLocationFile));
				
				editText.setText("");

			}
		});
		alert.setButton(Dialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				new File(tempSoundFile).delete();
				new File(tempLocationFile).delete();
				
				EditText editText = (EditText) alert.findViewById(R.id.new_file_name);
				editText.setText("");
				
				btnPlay.setEnabled(false);
			}
		});
		alert.setCancelable(false);

		return alert;
	}

	private void saveFile(String fileName, String fileContent) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(fileName)));
			out.write(fileContent);
			out.close();
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}

	private void setupMediaRecorderSettings() {
		mediaRecorderSettings = new MediaRecorderSettings();
		try {
    		    		
    		FileInputStream fis = openFileInput(MEDIA_RECORDER_SETTINGS_FILE_NAME);
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		byte[] buffer = new byte[512];
    		int bytes_read;
    		while((bytes_read = fis.read(buffer)) != -1) {
    			baos.write(buffer, 0, bytes_read);
    		}
    		
    		String data = new String(baos.toByteArray());
    		String[] pieces = data.split("#");
    		mediaRecorderSettings.setOutputType(Integer.parseInt(pieces[0]));
    		mediaRecorderSettings.setSamplingRate(Integer.parseInt(pieces[1]));
    		mediaRecorderSettings.setBitRate(Integer.parseInt(pieces[2]));
    		mediaRecorderSettings.setAudioEncoder(Integer.parseInt(pieces[3]));    		
    	}
    	catch(FileNotFoundException fnfe) {}
    	catch(Exception exp) {
			errorMessage = "Error Retrieving Audio Settings";
			showDialog(ERROR_DIALOG);
    	}
	}
	
	private void setupLocationServices() {
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}

			public void onLocationChanged(Location location) {

				double latitude = location.getLatitude();
				double longitude = location.getLongitude();

				if (!isPlaying) {
					currentPoint = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
					if (isRecording) {
						tempLocationBuffer += currentPoint.getLatitudeE6() + " " + currentPoint.getLongitudeE6() + " " + (System.currentTimeMillis() - recordStartTime) + "\n";
					}
					
					if(routeOverlay == null) {
						mapController.setCenter(currentPoint);
						mapView.getOverlays().remove(markerOverlay);
						markerOverlay.clearOverlays();
						OverlayItem overlayItem = new OverlayItem(currentPoint, "You are here", "You are here");
						markerOverlay.addOverlay(overlayItem);
						mapView.getOverlays().add(markerOverlay);
						
						mapView.invalidate();
					}
				}
			}
		};

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2.0f, locationListener);
		} else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 2.0f, locationListener);
		} else {
			showDialog(LOCATION_SETTINGS_DIALOG);
		}		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about_page:
			showDialog(ABOUT_PAGE_DIALOG);
			return true;
		case R.id.import_page:
			handleImportAction();
			return true;
		case R.id.export_page:
			showDialog(EXPORT_FILE_DIALOG);
			return true;
		case R.id.scupload_page:
			callSCUploadIntent();
			return true;
		case R.id.audiosettings_page:
			callAudioSettingsIntent();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void callSCUploadIntent() {
		try {
    		Intent uploadIntent = new Intent(this, SCUploadActivity.class);
    		uploadIntent.setAction("com.ahmetkizilay.audio.geotag.upload.SCUpload");
    		startActivityForResult(uploadIntent, UPLOAD_INTENT_RESULT);
    	}
    	catch(Exception exp) {
    		Toast.makeText(this, "Error calling the upload intent", Toast.LENGTH_SHORT).show();
    	}
	}
	
    private void callAudioSettingsIntent() {
    	try {
    		Intent audioSettingsIntent = new Intent(this, MediaRecorderSettingsActivity.class);
    		audioSettingsIntent.setAction("com.ahmetkizilay.audio.geotag.settings.MediaRecorderSettings");
    		//audioSettingsIntent.setClass("com.ahmetkizilay.audio.geotag.settings", MediaRecorderSettingsActivity.class);
    		audioSettingsIntent.putExtra("selectedOutputType", mediaRecorderSettings.getOutputType());
    		audioSettingsIntent.putExtra("selectedSamplingRate", mediaRecorderSettings.getSamplinRate());
    		audioSettingsIntent.putExtra("selectedBitRate", mediaRecorderSettings.getBitRate());
    		audioSettingsIntent.putExtra("selectedAudioEncoder", mediaRecorderSettings.getAudioEncoder());
    		
    		startActivityForResult(audioSettingsIntent, MEDIA_RECORDER_SETTINGS_INTENT_RESULT);
    	}
    	catch(Exception exp) {
    		Toast.makeText(this, "Error calling the intent", Toast.LENGTH_SHORT).show();
    	}
    }
	
	private void handleImportAction() {
		try {
			Intent intent = new Intent(this, ImportFileActivity.class);
			intent.setAction("com.ahmetkizilay.audio.geotag.IMPORT_FILE");
			startActivityForResult(intent, IMPORT_INTENT_RESULT);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {		
		super.onActivityResult(requestCode, resultCode, data);
		
		if(resultCode == Activity.RESULT_OK) {
			switch(requestCode) {
			case MEDIA_RECORDER_SETTINGS_INTENT_RESULT:
				updateMediaRecorderSettings(data);
				break;
			case IMPORT_INTENT_RESULT:
				handleImportResultAction(data);
			case UPLOAD_INTENT_RESULT:
				handleUploadResultAction(data);
			}
		}
	}
	
	private void handleUploadResultAction(Intent data) {
		
	}
		
    private void updateMediaRecorderSettings(Intent intent) {
    	
		int selectedOutputType = Integer.parseInt(intent.getExtras().get("selectedOutputType").toString());
		int selectedSamplingRate = Integer.parseInt(intent.getExtras().get("selectedSamplingRate").toString());
		int selectedBitRate = Integer.parseInt(intent.getExtras().get("selectedBitRate").toString());
		int selectedAudioEncoder = Integer.parseInt(intent.getExtras().get("selectedAudioEncoder").toString());

		mediaRecorderSettings.setOutputType(selectedOutputType);
		mediaRecorderSettings.setSamplingRate(selectedSamplingRate);
		mediaRecorderSettings.setBitRate(selectedBitRate);
		mediaRecorderSettings.setAudioEncoder(selectedAudioEncoder);
		
		UpdateMediaRecorderSettingsInternalStorage();		
    }
    
    private void UpdateMediaRecorderSettingsInternalStorage() {
    	try {
    		String data = mediaRecorderSettings.getOutputType() + "#" + 
    				      mediaRecorderSettings.getSamplinRate() + "#" +
    				      mediaRecorderSettings.getBitRate() + "#" +
    				      mediaRecorderSettings.getAudioEncoder();
    		FileOutputStream fos = openFileOutput(MEDIA_RECORDER_SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
    		fos.write(data.getBytes());
    		fos.close();
    	}
    	catch(Exception exp) {
    		Toast.makeText(this, "Could Not Update RecorderSettings File", Toast.LENGTH_SHORT).show();
    	}
    }

	private void handleImportResultAction(Intent data) {
		ZipInputStream zis = null;
		BufferedOutputStream bos = null;
		try {
			
			String selectedFile = data.getExtras().getString("selectedFile");
			zis = new ZipInputStream(new FileInputStream(new File(selectedFile)));
			for(ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
				if(entry.getName().endsWith(".txt") || entry.getName().endsWith(".3gp")) {
					String rootName = entry.getName();
					File newImportedFile = createNewImportedFile(rootName);
					
					FileOutputStream fos = new FileOutputStream(newImportedFile);
					bos = new BufferedOutputStream(fos, 1024);
					
					int bytes_read;
					byte[] buffer = new byte[1024];
					while((bytes_read = zis.read(buffer, 0, 1024)) != -1) {
						bos.write(buffer, 0, bytes_read);
					}
					
					bos.flush();
					bos.close();
				}
				zis.closeEntry();
			}
			zis.close();
			
			Toast.makeText(GeoTaggedAudioActivity.this, "Record Import Complete", Toast.LENGTH_LONG).show();
		}
		catch(Exception exp) {
			exp.printStackTrace();
			Toast.makeText(GeoTaggedAudioActivity.this, "Error While Importing File", Toast.LENGTH_LONG).show();
			if(bos != null) try { bos.close(); } catch(Exception ex) {}
			if(zis != null) try { zis.close(); } catch(Exception ex) {}
		}		
	}
	
	private File createNewImportedFile(String rootName) {
		
		File resultFile = new File(home_directory_string + "/" + rootName);
		if(!resultFile.exists()) {
			return resultFile;
		}
		else {
			String fileName = rootName.substring(0, rootName.length() - 4);
			String extension = rootName.substring(rootName.length() - 4);	
			int count = 1;
			do {
				resultFile = new File(home_directory_string + "/" + fileName + "_" + count + extension);
				count = count + 1;
			} while(resultFile.exists());
			
			return resultFile;
		}
		
	}
	
	private Dialog createAboutDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Version 1.2\n\nPERISONiC Sound And Media").setCancelable(false).setNeutralButton("Close", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
			}
		}).setPositiveButton("Follow Me", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				Intent twitterIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mobile.twitter.com/ahmetkizilay"));				
				startActivity(twitterIntent);				
			}
		}).setTitle("Geo-Tagged Audio").setIcon(R.drawable.geotagged_audio);
		
		return builder.create();
	}
	
	private void prepareRouteOverlay() {
		coordinates = CoordinateInfo.createCoordinateInfoListFromFile(selectedLocationFile);

		if (routeOverlay != null) {
			mapView.getOverlays().remove(routeOverlay);
		}

		routeOverlay = new RouteOverlay(coordinates);
		mapView.getOverlays().add(routeOverlay);
		mapController.setCenter(new GeoPoint(coordinates.get(0).getLatitude(), coordinates.get(0).getLongitude()));
		mapController.setZoom(17);
	}
	
	private void acquireWakeLock() {
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "com.ahmetkizilay.audio.geotag.GeotaggedAudio");
		wakeLock.acquire();
	}
	
	private void releaseWakeLock() {
		if(wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	}
	
	
	
	class CustomUncaughtExceptionHandler implements UncaughtExceptionHandler {
		public void uncaughtException(Thread thread, Throwable ex) {
			String message = ex.getMessage();
			System.out.println(message);
		}
		
	}
}