package com.smeup.iotspi.jd003;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Document;

public class VegaANPRClient extends Thread implements VegaANPRDataDocumentListenerInterface, VegaANPRLogEventListenerInterface{

	private ServerSocket iServerSocket = null;
	private int iPort;
	private boolean iActive;

	private ArrayList<VegaANPRLogEventListenerInterface> vLogListenerList = new ArrayList<VegaANPRLogEventListenerInterface>();
	private ArrayList<VegaANPRDataDocumentListenerInterface> vDataDocumentListenerList = new ArrayList<VegaANPRDataDocumentListenerInterface>();
	
	
	
	public VegaANPRClient(int aPort, boolean aActive){	
		this.setPort(aPort);
		this.setActive(aActive);
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
	
	
	public void setPort(int aPort){
		this.iPort = aPort;
	}
	
	
	public void setActive(boolean aActive){
		this.iActive = aActive;
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
	
	
	public void menageLogEvent(VegaANPRLogEvent e) {
		this.fowardLogEvent(e.getLevel(), e.getMessage());
	}
	

	public void menageEventDataDocument(VegaANPRDataDocumentEvent e) {
		this.fowardDataDocumentEvent(e.getDataDocument());
	}
	
	public void run() {
		try {
			fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaClient - Metodo run");
			
			// Istanzia il serverSocket
			iServerSocket = new ServerSocket(iPort);
			
			int vCounter= 0;
			iActive = true;
			
			BufferedReader vInBuffer = null;
			Socket iClientSocket = null;

			while (iActive && vCounter<1000) {
				try {
					fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaClient - Metodo run - Attesa connessione...");
					iClientSocket = iServerSocket.accept();
					fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaClient - Metodo run - Ricevuta connessione");
					
					vInBuffer = new BufferedReader(new InputStreamReader(iClientSocket.getInputStream()));
					fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaClient - Metodo run - Creazione BufferedReader");
					
					StringWriter vWriter = new StringWriter();
					String vResult = "";
					char[] vBuffer = new char[1024 * 32];
					int iReaded = 0;
					int vNChar = 0;
					
					// Se la read non riempie buffer per tot secondi viene lanciato il SocketTimeoutException
					iClientSocket.setSoTimeout(1000);
					
					while ((iReaded = vInBuffer.read(vBuffer)) != -1)
					{
						vWriter.write(vBuffer, 0, iReaded);
						vNChar+=iReaded;
					}
					
					fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaClient - Metodo run - Estratto buffer");
					vResult = vWriter.toString().trim();
					
					VegaANPRDocumentCreator reader = new VegaANPRDocumentCreator(vResult);
					reader.addLogEventListener(this);
					reader.addDataDocumentEventListener(this);
					reader.start();
					
					// Forza la chiusura del chiamante e del buffer
					socketAndInBufferDestroy(iClientSocket, vInBuffer);

					
				} catch (SocketTimeoutException vEx) {
					socketAndInBufferDestroy(iClientSocket, vInBuffer);
					fowardLogEvent(VegaANPRLogEventListenerInterface.T_ERR,"Errore classe VegaClient - Metodo run - SocketTimeout - " + vEx.getMessage());
					vCounter+=1;
				} catch (IOException vEx) {
					socketAndInBufferDestroy(iClientSocket, vInBuffer);
					fowardLogEvent(VegaANPRLogEventListenerInterface.T_ERR,"Errore classe VegaClient - Metodo run -  -" + vEx.getMessage());
					vCounter+=1;
				} catch (Exception vEx) {
					socketAndInBufferDestroy(iClientSocket, vInBuffer);
					fowardLogEvent(VegaANPRLogEventListenerInterface.T_ERR,"Errore classe VegaClient - Metodo run in While Loop -" + vEx.getMessage());
					vCounter+=1;
				}
			}
			
			if(vCounter>=1000)
			{
                fowardLogEvent(VegaANPRLogEventListenerInterface.T_ERR,"Errore classe VegaClient - Provato avvio VegaANPRReader per "+vCounter+". Lascio perdere. VegaClient non attivo.");
                iActive = false;			    
			}
		}
		catch (Exception vEx) {
			iActive = false;
			fowardLogEvent(VegaANPRLogEventListenerInterface.T_ERR,"Errore classe VegaClient - Metodo run - " + vEx.getMessage());
		}
	}
	
	
	public synchronized void destroy()
	{
	    try
        {
            iServerSocket.close();
        }
        catch(IOException ex)
        {
            fowardLogEvent(VegaANPRLogEventListenerInterface.T_ERR,"Errore classe VegaClient - Metodo stop - " +ex.getMessage());
        }
	}
	
	public void socketAndInBufferDestroy(Socket aClientSocket, BufferedReader aInBuffer) throws IOException{
		if(aInBuffer != null){
			aInBuffer.close();
		}
		fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaClient - Metodo run - Chiuso Buffer con il chiamante");
		if(aClientSocket != null){
			aClientSocket.close();
		}
		fowardLogEvent(VegaANPRLogEventListenerInterface.T_DBG,"Classe VegaClient - Metodo run - Chiusa la socket");
	}
}
