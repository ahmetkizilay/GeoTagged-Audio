package com.ahmetkizilay.audio.geotag.upload;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

public class HttpPoster {
	private static final String HTTP_GEOTAG_AUDIO_POST = "http://www.ahmetkizilay.com/gtagau/fu.php";
	
	private String fileName;
	private String userName;
	private String trackId;
	private String trackTitle;
	private String errorMessage;
	
	public HttpPoster(String fileName, String userName, String trackId, String trackTitle) {
		this.fileName = fileName;
		this.userName = userName;
		this.trackId = trackId;
		this.trackTitle = trackTitle;
		this.errorMessage = "";
	}
	
	public String getErrorMessage() {
		return this.errorMessage;
	}
	
	public boolean post() {
		try {
			HttpClient httpClient = new DefaultHttpClient();
			httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
			
			HttpPost httpPost = new HttpPost(HTTP_GEOTAG_AUDIO_POST);
			File file = new File(fileName);
			
			MultipartEntity mpEntity = new MultipartEntity();
			ContentBody cbFile = new FileBody(file);
			mpEntity.addPart("userFile", cbFile);
			mpEntity.addPart("scUserName", new StringBody(this.userName));
			mpEntity.addPart("scTrackId", new StringBody(this.trackId));
			mpEntity.addPart("scTrackTitle", new StringBody(this.trackTitle));
			httpPost.setEntity(mpEntity);
			
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity resEntity = response.getEntity();
			if(response.getStatusLine().getStatusCode() == 200) {
				InputStream responseStream = resEntity.getContent();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[512];
				int bytes_read;
				while((bytes_read = responseStream.read(buffer)) != -1) {
					baos.write(buffer, 0, bytes_read);
				}
				
				String responseString = new String(baos.toByteArray());
				if(responseString.startsWith("OK")) {
					return true;
				}
				else {			
					this.errorMessage = responseString.replace("ERROR ", "");
					return false;
				}				
			}
			return true;
		}
		catch(Exception exp) {
			this.errorMessage = exp.getMessage();
			return false;
		}
	}
}
