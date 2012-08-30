package com.ahmetkizilay.audio.geotag.settings;

public class MediaRecorderSettings {
	private boolean boolSamplingRate = false;
	private int samplingRate;
	
	private boolean boolBitRate = false;
	private int bitRate;
	
	private boolean boolAudioEncoder = false;
	private int audioEncoder;
	
	private boolean boolOutputType = false;
	private int outputType;
	
	public MediaRecorderSettings() {
		
	}
	
	public boolean isSamplingRateSet() {
		return boolSamplingRate;
	}
	
	public int getSamplinRate() {		
		return this.samplingRate;
	}
	
	public void setSamplingRate(int samplingRate) {
		this.boolSamplingRate = samplingRate >= 0;
		this.samplingRate = samplingRate;
	}
	
	public boolean isBitRateSet() {
		return boolBitRate;
	}
		
	public int getBitRate() {
		return this.bitRate;
	}
	
	public void setBitRate(int bitRate) {
		this.boolBitRate = bitRate >= 0;
		this.bitRate = bitRate;
	}
	
	public boolean isAudioEncoderSet() {
		return boolAudioEncoder;
	}
		
	public int getAudioEncoder() {
		return this.audioEncoder;
	}
	
	public void setAudioEncoder(int audioEncoder) {
		this.boolAudioEncoder = audioEncoder >= 0;
		this.audioEncoder = audioEncoder;
	}
	
	public boolean isOutputTypeSet() {
		return boolOutputType;
	}
		
	public int getOutputType() {
		return this.outputType;
	}
	
	public void setOutputType(int outputType) {
		this.boolOutputType = outputType >= 0;
		this.outputType = outputType;
	}
	
	
	
	
}
