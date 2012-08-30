package com.ahmetkizilay.audio.geotag.upload;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import com.ahmetkizilay.audio.geotag.R;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Env;
import com.soundcloud.api.Http;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SCUploadActivity extends Activity{

	private static final int FILE_CHOOSER_DIALOG = 1;
	private static final int UPLOAD_PROGRESS_DIALOG = 2;
	private static final int TRACK_INFO_DIALOG = 3;
	
	private static final String SCAUTH_TOKEN_FILE = "scauth_file";

	private static final String CLIENT_ID = "eca5e5e0sadasdc2100fd49d26";
	private static final String CLIENT_SECRET = "2asdasdasdaasdasdasdasdc";


	private Button loginButton;
	private Button uploadButton;
	private ProgressBar waitProgress;
	private TextView loginUserText;
	
	private ProgressDialog progressDialog;
	private UploadProgressThread progressThread;
	
	private Token scToken;
	private ApiWrapper scWrapper;
	private String loggedInUser;
		
	private String selectedAudioFileName;
	private String selectedGeoDataFileName;
	private String trackDescription = "";
	private String trackTags = "";
	private String trackTitle = "";
	
	private String home_file_directory;
	
	private PowerManager.WakeLock wakeLock;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sc_upload);
		
		readSCTokenFromStore();
		if(getIntent().getDataString() != null) {
			handleSCAuthCallback();
		}
		
		loginButton = (Button) findViewById(R.id.btnLogin_upl);
		uploadButton = (Button) findViewById(R.id.btnUpload_upl);
		waitProgress = (ProgressBar) findViewById(R.id.pbWait_upl);
		loginUserText = (TextView) findViewById(R.id.twLoggedIn_upl);
		
		loginButton.setEnabled(false);
		uploadButton.setEnabled(false);
		
		waitProgress.setVerticalFadingEdgeEnabled(true);
		waitProgress.setVisibility(ProgressBar.VISIBLE);
		
		loginButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				handleLoginClick();
			}
		});
		
		uploadButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				handleUploadClick();
			}
		});
		
		home_file_directory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/perisonic/audio";

		new InitializeThread(initHandler).start();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case FILE_CHOOSER_DIALOG:
			return createFileChooserDialog();		
		case UPLOAD_PROGRESS_DIALOG:
			return createUploadProgressDialog();
		case TRACK_INFO_DIALOG:
			return createTrackInfoDialog();
		default:
			return super.onCreateDialog(id);
		}
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id) {
		case UPLOAD_PROGRESS_DIALOG:
			handlePrepareUploadProgressDialog();
		default:
			super.onPrepareDialog(id, dialog);
		}
	}
	
	final Handler initHandler = new Handler() {
		
		public void handleMessage(Message msg) {
			switch(msg.arg1) {
			case InitializeThread.AUTH_SUCCESS:
				loggedInUser = msg.obj.toString();
				loginUserText.setText("User: " + loggedInUser);
				
				loginButton.setText("ReLogin");
			
				uploadButton.setEnabled(true);
				loginButton.setEnabled(true);
				break;
			case InitializeThread.TOKEN_EXPIRED:
				loginUserText.setText("Token Expired");
				loginButton.setText("Login");
			
				loginButton.setEnabled(true);
				break;
			case InitializeThread.TOKEN_NOT_PRESENT:
				loginUserText.setText("Please Login");
				loginButton.setText("Login");
			
				loginButton.setEnabled(true);
				break;
			case InitializeThread.SOME_OTHER_ERROR:
				loginUserText.setText("Please Login");
				loginButton.setText("Login");
			
				loginButton.setEnabled(true);
				Toast.makeText(SCUploadActivity.this, "Error: " +  msg.obj.toString(), Toast.LENGTH_SHORT).show();
				break;
			}
				
			waitProgress.setVisibility(ImageView.INVISIBLE);
		}
	};
	
	// runs at initialization
	// retrieves username
	private class InitializeThread extends Thread {
		public static final int AUTH_SUCCESS = 0;
		public static final int TOKEN_NOT_PRESENT = 1;
		public static final int TOKEN_EXPIRED = 2;		
		public static final int SOME_OTHER_ERROR = 3;
		
		Handler mHandler;
		
		
		InitializeThread(Handler handle) {
			this.mHandler = handle;
		}
		
		public void run() {			
			try {
				// if token null, do nothing, just say not logged in
				// if token is not null, get username
				// if username retrieve fails, ask to login again
				if(scToken == null) {
					Message msg = new Message();
					msg.arg1 = TOKEN_NOT_PRESENT;
					mHandler.sendMessage(msg);
				}
				else {
					scWrapper = new ApiWrapper(CLIENT_ID, CLIENT_SECRET, new URI("gtag-audio://soundcloud/redirect"), scToken, Env.LIVE);
					HttpResponse resp = scWrapper.get(Request.to("/me"));
					if(resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						JSONObject jsonResponse = Http.getJSON(resp);
						
						scToken = scWrapper.getToken();
						writeSCAuthTokenToStore();
						
						Message msg = new Message();
						msg.arg1 = AUTH_SUCCESS;
						msg.obj = jsonResponse.getString("username");

						mHandler.sendMessage(msg);
					}
					else {
						Message msg = new Message();
						msg.arg1 = TOKEN_EXPIRED;
						mHandler.sendMessage(msg);
					}
				}
			}
			catch(java.net.UnknownHostException uhe) {
				uhe.printStackTrace();
				
				Message msg = new Message();
				msg.arg1 = SOME_OTHER_ERROR;
				msg.obj = "Check Internet Connection";
				mHandler.sendMessage(msg);
			}
			catch(Exception exp) {
				exp.printStackTrace();
				
				Message msg = new Message();
				msg.arg1 = SOME_OTHER_ERROR;
				msg.obj = "Please try again";
				mHandler.sendMessage(msg);
			}
		}
	}
	
	private void writeSCAuthTokenToStore() {
		try {
    		FileOutputStream fos = openFileOutput(SCAUTH_TOKEN_FILE, Context.MODE_PRIVATE);
    		
    		JSONObject json = new JSONObject();
    		json.accumulate(Token.ACCESS_TOKEN, this.scToken.access);
    		json.accumulate(Token.EXPIRES_IN, this.scToken.expiresIn);
    		json.accumulate(Token.REFRESH_TOKEN, this.scToken.refresh);
    		json.accumulate(Token.SCOPE, this.scToken.scope);
    		
    		fos.write(json.toString().getBytes());
    		fos.close();
    	}
    	catch(Exception exp) {
    		Toast.makeText(this, "Could Not Update SCAuth File", Toast.LENGTH_SHORT).show();
    		exp.printStackTrace();
    	}
	}
	
	private void readSCTokenFromStore() {
		try {	
    		FileInputStream fis = openFileInput(SCAUTH_TOKEN_FILE);
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		byte[] buffer = new byte[512];
    		int bytes_read;
    		while((bytes_read = fis.read(buffer)) != -1) {
    			baos.write(buffer, 0, bytes_read);
    		}
    		
    		this.scToken = new Token(new JSONObject(new String(baos.toByteArray())));
    		    		
    	}
    	catch(FileNotFoundException fnfe) {}
    	catch(Exception exp) {
    		Toast.makeText(this, "Could Not Read SCAuth File", Toast.LENGTH_SHORT).show();
    		Log.e("ERROR", "Could Not Read SCAuth File");
    	}
	}
	
	private void handleSCAuthCallback() {
		try {
			Uri calbackUri = getIntent().getData();
			if(calbackUri.getScheme().equalsIgnoreCase("gtag-audio")) {
				String authTokenCode = calbackUri.getQueryParameter("code");
				ApiWrapper tmpWrapper = new ApiWrapper(CLIENT_ID, CLIENT_SECRET, new URI("gtag-audio://soundcloud/redirect"), null, Env.LIVE);
				this.scToken = tmpWrapper.authorizationCode(authTokenCode);

				writeSCAuthTokenToStore();
			}
		}
		catch(Exception exp) {
			Toast.makeText(this, "Handle SCAuth Callback", Toast.LENGTH_LONG).show();
		}
	}
	
	private void handleLoginClick()  {
		try {
			if(this.scWrapper != null) {
				this.scWrapper.invalidateToken();
			}
			else {
				this.scWrapper = new ApiWrapper(CLIENT_ID, CLIENT_SECRET, new URI("gtag-audio://soundcloud/redirect"), null, Env.LIVE);
			}
			
			URI uri = scWrapper.authorizationCodeUrl();
			Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString()));
			startActivity(viewIntent);
		}
		catch(Exception exp) {
			Toast.makeText(this, "Error SC Re-Auth", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void handleUploadClick() {
		showDialog(FILE_CHOOSER_DIALOG);
	}
	
	public Dialog createFileChooserDialog() {		
		File homeDirectory = new File(home_file_directory);
		final String[] fileNames = homeDirectory.list(new FilenameFilter() {

			public boolean accept(File dir, String filename) {
				if (filename.endsWith(".3gp")) {
					String textFileName = filename.replace(".3gp", ".txt");
					if (new File(home_file_directory + "/" + textFileName).exists()) {
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
		builder.setTitle("Choose A File To Import");
		
		builder.setOnCancelListener(new OnCancelListener() {			
			public void onCancel(DialogInterface dialog) {
				removeDialog(FILE_CHOOSER_DIALOG);
			}
		});
		builder.setItems(fileNames, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				selectedAudioFileName = fileNames[item]  + ".3gp";
				selectedGeoDataFileName = fileNames[item]  + ".txt";
				
				removeDialog(FILE_CHOOSER_DIALOG);

				showDialog(TRACK_INFO_DIALOG);

			}
		});
		return builder.create();
	}
	
	private Dialog createTrackInfoDialog() {
		LayoutInflater factory = LayoutInflater.from(this);
		final View trackInfoView = factory.inflate(R.layout.upload_text, null);
		final AlertDialog alert = new AlertDialog.Builder(SCUploadActivity.this).create();
		alert.setView(trackInfoView);
		alert.setTitle("Track Info");
		alert.setCancelable(false);
//		alert.setButton(AlertDialog.BUTTON_NEUTRAL, "Skip", new DialogInterface.OnClickListener() {
//			
//			public void onClick(DialogInterface dialog, int which) {
//				
//				trackDescription = "";
//				trackTags = "";
//				
//				removeDialog(TRACK_INFO_DIALOG);	
//				showDialog(UPLOAD_PROGRESS_DIALOG);
//			}
//		});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				trackDescription = "";
				trackTags = "";
				
				selectedAudioFileName = "";
				selectedGeoDataFileName = "";
				
				removeDialog(TRACK_INFO_DIALOG);
			}
		});
		alert.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				EditText etTitle = (EditText) alert.findViewById(R.id.etTitle_ti);
				EditText etDescr = (EditText) alert.findViewById(R.id.etDescr_ti);
				EditText etTags = (EditText) alert.findViewById(R.id.etTags_ti);
				
				String enteredTitle = etTitle.getText().toString();
				
				trackDescription = etDescr.getText().toString();
				trackTags = etTags.getText().toString();
				trackTitle = (enteredTitle.equals("") ?  selectedGeoDataFileName.replace(".txt", "") : enteredTitle); 
				
				removeDialog(TRACK_INFO_DIALOG);
				showDialog(UPLOAD_PROGRESS_DIALOG);

			}
		});
		return alert;
	}
	
	
	private Dialog createUploadProgressDialog() {
		progressDialog = new ProgressDialog(SCUploadActivity.this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMessage("Uploading Audio File...");
		return progressDialog;
	}
	
	private void handlePrepareUploadProgressDialog() {
		progressDialog.setMessage("Uploading Audio File...");
		progressDialog.setCancelable(false);
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.setProgress(0);
		
		acquireWakeLock();
		
		progressThread = new UploadProgressThread(uploadHandler);
		progressThread.start();
	}
	
	final Handler uploadHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.arg2) {
			case UploadProgressThread.MESSAGE_UPLOAD_GEOTAG_DATA:
				progressDialog.setMessage("Uploading GeoTag Data...");				
				break;
			case UploadProgressThread.MESSAGE_UPLOAD_DONE:
				dismissProgressDialogWithMessage("Your track '" + (String) msg.obj + "' is uploaded.");
				break;
			case UploadProgressThread.MESSAGE_UPLOAD_ERROR:
				dismissProgressDialogWithMessage("Error: '" + (String) msg.obj + "'");
				break;
			case UploadProgressThread.MESSAGE_UPLOAD_UPDATE:
				progressDialog.setProgress(msg.arg1);
				if(msg.arg1 >= 100) {
					progressDialog.setMessage("Transcoding Audio File...");					
				}
				break;
			}
		}
	};
	
	private void dismissProgressDialogWithMessage(String message) {
		
		progressDialog.setProgress(0);
		progressDialog.setMessage("Uploading Audio Data...");
		
		dismissDialog(UPLOAD_PROGRESS_DIALOG);
		
		releaseWakeLock();
		Toast.makeText(SCUploadActivity.this, message, Toast.LENGTH_LONG).show();
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
	protected void onStop() {
		super.onStop();
		
		releaseWakeLock();
	}
	
	private class UploadProgressThread extends Thread {
		public static final int MESSAGE_UPLOAD_GEOTAG_DATA = 0;
		public static final int MESSAGE_UPLOAD_UPDATE = 1;
		public static final int MESSAGE_UPLOAD_DONE = 2;
		public static final int MESSAGE_UPLOAD_ERROR = 3;

		Handler mHandler;
		
		UploadProgressThread(Handler handler) {
			this.mHandler = handler;
		}
		
		public void run() {
			try {

				File uplFile = new File(home_file_directory + "/" + selectedAudioFileName);
				final long totalFileLength = uplFile.length();
				scWrapper.getHttpClient().getParams().setParameter("http.socket.timeout", new Integer(300000));				
				HttpResponse uplResp = scWrapper.post(Request.to(Endpoints.TRACKS)
						.add(Params.Track.TITLE, trackTitle)
						.add(Params.Track.TAG_LIST, trackTags)
						.add(Params.Track.DESCRIPTION, trackDescription + " http://www.ahmetkizilay.com/gtagau")		
						.withFile(Params.Track.ASSET_DATA, uplFile)
						.setProgressListener(new UploadTransferListener(totalFileLength, this.mHandler)));
								
                if (uplResp.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                    JSONObject jsonResult = Http.getJSON(uplResp);
                    Message msg = new Message();
                    msg.arg2 = MESSAGE_UPLOAD_GEOTAG_DATA;
                    mHandler.sendMessage(msg);
                    
                    HttpPoster poster = new HttpPoster(home_file_directory + "/" + selectedGeoDataFileName, loggedInUser, jsonResult.getString("id"), jsonResult.getString("title"));
                    if(poster.post()) {
                    	msg = new Message();
                        msg.obj = jsonResult.get("title");
                        msg.arg2 = MESSAGE_UPLOAD_DONE;
                        mHandler.sendMessage(msg);
                    }
                    else {
                    	msg = new Message();
                        msg.obj = poster.getErrorMessage();
                        msg.arg2 = MESSAGE_UPLOAD_ERROR;
                        mHandler.sendMessage(msg);
                    }
                    
                } else {
                    Message msg = new Message();
                    msg.obj = uplResp.getStatusLine();
                    msg.arg2 = MESSAGE_UPLOAD_ERROR;
                    mHandler.sendMessage(msg);
                }
                
                scToken = scWrapper.getToken();
                writeSCAuthTokenToStore();                
			}
			catch(Exception exp) {
				exp.printStackTrace();
                Message msg = new Message();
                msg.obj = exp.getMessage();
                msg.arg2 = MESSAGE_UPLOAD_ERROR;
                mHandler.sendMessage(msg);
			}
		}
	}
	
	private class UploadTransferListener implements Request.TransferProgressListener {
		private long totalFileLength;
		private Handler mHandler;
		UploadTransferListener(long totalLength, Handler h) {
			this.totalFileLength = totalLength;
			this.mHandler = h;
		}
		
		public void transferred(long trans) throws IOException {
			Message msg = new Message();
			msg.arg1 = (int) Math.round(((float)trans / (float)totalFileLength) * 100.0);
			msg.arg2 = UploadProgressThread.MESSAGE_UPLOAD_UPDATE;
			mHandler.sendMessage(msg);			
		}
		
	}
}
