package com.smeup.IOTSPI_VegaANPR;

public interface VegaANPRLogEventListenerInterface {
	
	public static final int T_DBG = 0;
	public static final int T_INFO = 10;
	public static final int T_ERR = 50;	
	
	public abstract void menageLogEvent(VegaANPRLogEvent e);

}
