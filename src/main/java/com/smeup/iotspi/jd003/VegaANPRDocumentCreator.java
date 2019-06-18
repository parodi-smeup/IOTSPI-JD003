package com.smeup.iotspi.jd003;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;


public class VegaANPRDocumentCreator extends Thread {
	
	String iResult;

	private ArrayList<VegaANPRLogEventListenerInterface> vLogListenerList = new ArrayList<VegaANPRLogEventListenerInterface>();
	private ArrayList<VegaANPRDataDocumentListenerInterface> vDataDocumentListenerList = new ArrayList<VegaANPRDataDocumentListenerInterface>();
	
	public VegaANPRDocumentCreator(String aResult){
		this.iResult = aResult;
	}
	
	public void addLogEventListener(VegaANPRLogEventListenerInterface aLogListener){
		getLogListenerList().add(aLogListener);
	}
	
	
	public ArrayList<VegaANPRLogEventListenerInterface> getLogListenerList(){
		return vLogListenerList;
	}
	
	
	public void addDataDocumentEventListener(VegaANPRDataDocumentListenerInterface aDataDocumentListener){
		getDataDocumentListenerList().add(aDataDocumentListener);
	}
	
	
	public ArrayList<VegaANPRDataDocumentListenerInterface> getDataDocumentListenerList(){
		return vDataDocumentListenerList;
	}
	
	
	public void fowardLogEvent(int aLevelLog, String aLogMessage)
	{
		VegaANPRLogEvent iLogEvent = new VegaANPRLogEvent();
		iLogEvent.setLogLevel(aLevelLog);
		iLogEvent.setMessage(aLogMessage);
		for(Iterator<VegaANPRLogEventListenerInterface> iterator = getLogListenerList().iterator(); iterator.hasNext();){
			VegaANPRLogEventListenerInterface vLogEventListener = iterator.next();
			vLogEventListener.menageLogEvent(iLogEvent);
		}
	}
	
	
	public void fowardDataDocumentEvent(Document aDoc)
	{
		VegaANPRDataDocumentEvent iDataDocEvent = new VegaANPRDataDocumentEvent();
		iDataDocEvent.setDataDocument(aDoc);
		for(Iterator<VegaANPRDataDocumentListenerInterface> iterator = getDataDocumentListenerList().iterator(); iterator.hasNext();){
			VegaANPRDataDocumentListenerInterface vDataDocumentListenerList = iterator.next();
			vDataDocumentListenerList.menageEventDataDocument(iDataDocEvent);
		}
	}
	
	public void run()
	{
		try
		{

			fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaDocumentCreator - Metodo run");
			
			fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaDocumentCreator - Metodo run - Creazione Documento");
			
			Document vDoc= null;
			SAXReader xmlReader = new SAXReader();
			vDoc = xmlReader.read(new InputSource(new StringReader(iResult)));
			
			fowardDataDocumentEvent(vDoc);
			fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaDocumentCreator - Metodo run - Invio Documento creato");
			
			return;
		}
		catch(Exception vEx)
		{
			fowardLogEvent(VegaANPRLogEventListenerInterface.T_ERR,"Errore classe VegaDocumentCreator - Metodo run - " + vEx.getMessage());
		}
	}
}
