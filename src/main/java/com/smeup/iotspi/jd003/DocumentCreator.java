package com.smeup.iotspi.jd003;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;


public class DocumentCreator{
	
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
		String msgLog = "ForwardDataDocumentEvent...";
		System.out.println(msgLog);
		DataDocumentEvent iDataDocEvent = new DataDocumentEvent();
		iDataDocEvent.setDataDocument(aDoc);
		for(Iterator<DataDocumentListenerInterface> iterator = getDataDocumentListenerList().iterator(); iterator.hasNext();){
			msgLog = "manageEvent...";
			System.out.println(msgLog);
			DataDocumentListenerInterface vDataDocumentListenerList = iterator.next();
			vDataDocumentListenerList.menageEventDataDocument(iDataDocEvent);
			msgLog = "...done";
			System.out.println(msgLog);
		}
		msgLog = "...done";
		System.out.println(msgLog);
	}
	
	public void run()
	{
		try
		{
			String msgLog = "Create document from iResult:" + iResult;
			System.out.println(msgLog);
			
			Document vDoc= null;
			SAXReader xmlReader = new SAXReader();
			vDoc = xmlReader.read(new InputSource(new StringReader(iResult)));
			
			msgLog = "...done";
			System.out.println(msgLog);
			
			fowardDataDocumentEvent(vDoc);
			
			return;
		}
		catch(Exception vEx)
		{
			vEx.printStackTrace();
		}
	}
}
