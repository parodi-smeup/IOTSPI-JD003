package com.smeup.IOTSPI_VegaANPR;

public class VegaANPRLogEvent {

	private int iLevel;
	private String iMessage;
	
	
	public void setLogLevel(int aLevel){
		this.iLevel = aLevel;
	}
	
	
	public void setMessage(String aMessage){
		this.iMessage = aMessage;
	}
	
	
	public String getMessage(){
		return this.iMessage;
	}
	
	
	public int getLevel(){
		return this.iLevel;
	}
}
