package com.smeup.iotspi.jd003;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;


public class DocumentCreator extends Thread {
	
	String iResult;

	private ArrayList<DataDocumentListenerInterface> vDataDocumentListenerList = new ArrayList<DataDocumentListenerInterface>();
	
	public DocumentCreator(String aResult){
		this.iResult = aResult;
	}
	
	
	public void addDataDocumentEventListener(DataDocumentListenerInterface aDataDocumentListener){
		getDataDocumentListenerList().add(aDataDocumentListener);
	}
	
	
	public ArrayList<DataDocumentListenerInterface> getDataDocumentListenerList(){
		return vDataDocumentListenerList;
	}
	
	public void fowardDataDocumentEvent(Document aDoc)
	{
		DataDocumentEvent iDataDocEvent = new DataDocumentEvent();
		iDataDocEvent.setDataDocument(aDoc);
		for(Iterator<DataDocumentListenerInterface> iterator = getDataDocumentListenerList().iterator(); iterator.hasNext();){
			DataDocumentListenerInterface vDataDocumentListenerList = iterator.next();
			vDataDocumentListenerList.menageEventDataDocument(iDataDocEvent);
		}
	}
	
	public void run()
	{
		try
		{
			Document vDoc= null;
			SAXReader xmlReader = new SAXReader();
			vDoc = xmlReader.read(new InputSource(new StringReader(iResult)));
			
			fowardDataDocumentEvent(vDoc);
			
			return;
		}
		catch(Exception vEx)
		{
			vEx.printStackTrace();
		}
	}
}
