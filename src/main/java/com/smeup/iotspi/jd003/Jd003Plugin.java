package com.smeup.iotspi.jd003;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import org.dom4j.Document;

import Smeup.smeui.iotspi.datastructure.interfaces.SezConfInterface;
import Smeup.smeui.iotspi.datastructure.interfaces.SezInterface;
import Smeup.smeui.iotspi.datastructure.interfaces.SubConfInterface;
import Smeup.smeui.iotspi.datastructure.interfaces.SubInterface;
import Smeup.smeui.iotspi.datastructure.interfaces.SubMessageInterface;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorConf;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorInput;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorResponse;
import Smeup.smeui.iotspi.interaction.SPIIoTConnectorAdapter;
import Smeup.smeui.iotspi.interaction.SPIIoTEvent;
import Smeup.smeui.iotspi.interfaces.SPIIoTFrameworkInterface;

public class Jd003Plugin extends SPIIoTConnectorAdapter implements VegaANPRLogEventListenerInterface, VegaANPRDataDocumentListenerInterface{
	
	private SezInterface iSez = null;
	private IoTConnectorConf iConfiguration = null;
	private VegaANPRClient iClient = null;
	
	public int iPort = 8888;
	
	private String iSubId = "";
	
	public boolean isDebugActive= false;


	
	public Hashtable<String,EventComponent> iEventList = new Hashtable<String,EventComponent>();
	
	public IoTConnectorResponse invoke(IoTConnectorInput aDataTable) {
		String vResult = "";
		String vErrMsg = "";
		
		/*
		 * flag che indica se la risposta prevede la lettura di variabili (TRUE)
		 * oppure solo l'esito del comando (FALSE)
		 * 
		 */
		boolean vRespData = false;
		boolean vRespError = false;
		IoTConnectorResponse vResp= new IoTConnectorResponse();
		IoTConnectorResponse vRespErr= new IoTConnectorResponse();
		try
		{
			if(aDataTable!=null)
			{
				Set<String> vDataKeys = aDataTable.getKeys();
				for (String vKey : vDataKeys) {
					String vValue = aDataTable.getData(vKey);
					if (vValue.trim().equalsIgnoreCase("*READ"))
					{	
						// Richiesta lettura della variabile vKey
						vResult=iEventList.get(vKey).getIValue();

						vResp.addData(vKey, vResult);
						vRespData=true;
					}
					else if (iEventList.get(vKey).getIType() == EventComponent.CMD)
					{	
						// Richiesta scrittura del valore vValue alla variabile vKey
						// gestire eventuale ritorno
						try {
							iEventList.get(vKey).setICMD(vValue);
						}
						catch(Exception vEx) {
							log(VegaANPRLogEventListenerInterface.T_ERR,"Errore metodo setICMD - " + vEx.getMessage());
							throw new Exception("Errore metodo setICMD - " + vEx.getMessage());
						}	
					}
					else
					{	
						// Test se variabile di tipo IN --> scrittura non consentita
						if(iEventList.get(vKey).getIType() == EventComponent.IN) {
							vRespError=true;
							log(VegaANPRLogEventListenerInterface.T_ERR,"Metodo invoke - SUB [ " + iSubId + " ] - Scartata richiesta variabile " + vKey + " perche' di solo INPUT");
							continue;
						}
						// Richiesta scrittura del valore vValue alla variabile vKey
						iEventList.get(vKey).setIValue(vValue);
					}
				}

				if(vRespError && !(vResp.getKeys().hasMoreElements()))
					throw new Exception("Solo variabili non ammesse");
				if (!vRespData)
				{
					vResult="OK";
					vResp.addData("RESULT", vResult);
					log(VegaANPRLogEventListenerInterface.T_INFO,"Risposta su INVOKE - " + vResult);
				}
			}
			else
				throw new Exception("Ricevuto messaggio vuoto"); 
		}
		catch (Exception vEx)
		{
			vResult="KO";
			vRespErr.addData("RESULT", vResult);
			vErrMsg=vEx.getMessage();
			vRespErr.addData("ERROR", vErrMsg);
			log(VegaANPRLogEventListenerInterface.T_ERR,"Metodo INVOKE - " + vErrMsg);
			return vRespErr;
		}
		//controllo congruenza lista variabili con elenco messaggi
		return vResp;
	}

	
	public boolean ping() {
		return true;
	}

	
	public boolean unplug() {
		try {
			destroy();
		}
		catch(Exception vEx) {
			log(VegaANPRLogEventListenerInterface.T_ERR,"Errore metodo unplug - " + vEx.getMessage());
			return false;
		}
		return true;
	}

	
	public boolean isDebugActive() {
		return isDebugActive;
	}

	
	public void log(int aLevel, String aText)
	{
		if(this.isDebugActive()  || (aLevel >= VegaANPRLogEventListenerInterface.T_INFO)){
                    if (iSez != null) {
                        iSez.log(aText);
                    }			
		}
	}
	
	
	@Override
	public boolean postInit(SezInterface aSez, IoTConnectorConf aConfiguration) {
		try {

			iSez = aSez;
			iConfiguration = aConfiguration;
			
			log(VegaANPRLogEventListenerInterface.T_INFO,"Metodo init - " + aSez.getName());

			// Lettura di parametro di debug attivo
			if(iConfiguration != null && iConfiguration.getData("Debug")!= null &&  iConfiguration.getData("Debug").equalsIgnoreCase("true")){
				isDebugActive = true;
			}
			
			
			// Lettura parametri di connessione
			initParams();			
			String vPort = iConfiguration.getData("Port");
                        
			log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo init - vPort=" + vPort);
						
			if(vPort != null && !vPort.isEmpty())
			{
				try
				{
					iPort = Integer.parseInt(vPort);
					log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo init - iPort=" + iPort);
				}
				catch (NumberFormatException vEx)
				{
					log(VegaANPRLogEventListenerInterface.T_ERR,"Errore metodo init - Set Porta");
					initParams();
				}
			}
			
			
			// Lettura SUB con variabili
			// Questo plug-in prevede l'implementazione di UNA SOLA SUB
			ArrayList<SubInterface> vSubList = aConfiguration.getSubList();
			
			SubInterface vSub = vSubList.get(0);
			SubConfInterface vSubConf = vSub.getConf();
			iSubId = vSub.getId();
			
			//tabella di tutte le variabili del plug-in
			ArrayList<Hashtable<String, String>> vSubVarTable = vSubConf.getConfTable();

			log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo init - Lettura " + vSubVarTable.size() + " variabili");
			
			for (int i = 0; i < vSubVarTable.size(); i++) {

				String vEvtNome = vSubVarTable.get(i).get("Name");
				
				if(iEventList.get(vEvtNome) != null) {
					// Fare LOG per variabile saltata per ID duplicato
					log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo init - Variabile " + vEvtNome + " in " + iSubId + " DUPLICATA");
				}
				else {
					EventComponent vEvtComp = new EventComponent(vEvtNome);
					log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo init - Lettura variabile " + vEvtNome);
					
					vEvtComp.setIEventName(vEvtNome);
					vEvtComp.setIEventDesc(vSubVarTable.get(i).get("Txt"));
					vEvtComp.setIDataType(vSubVarTable.get(i).get("TpDato"));
					vEvtComp.setIType(vSubVarTable.get(i).get("TpVar"));
					vEvtComp.setIDftValue(vSubVarTable.get(i).get("DftVal"));
					vEvtComp.setIHowRead(vSubVarTable.get(i).get("HowRead"));

					iEventList.put(vEvtNome, vEvtComp);
				}
			}

			// Corrispondenza tabella SUBVAR e MSGVAR
			SubMessageInterface vSubMsg = vSub.getMessage();
			ArrayList<Hashtable<String, String>> vMsgVarTable = vSubMsg.getConfTable();
			log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo init - Lettura " + vMsgVarTable.size() + " variabili messaggio");
			
			for (int i = 0; i < vMsgVarTable.size(); i++) {
				try {

					String vMsgNome = vMsgVarTable.get(i).get("Name");
					String vMsgIO = vMsgVarTable.get(i).get("IO");
					log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo init - Lettura variabile messaggio " + vMsgNome);
					
					boolean vMsgIObool = false;
					if(vMsgIO.contains("I")) vMsgIObool = true;

					(iEventList.get(vMsgNome)).setIsMsgRet(vMsgIObool);
				}
				catch (Exception vEx) {
					log(VegaANPRLogEventListenerInterface.T_ERR,"Errore metodo init - Lettura variabili messaggio");
					return false;
				}
			}
			log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo init - Creazione VegaClient");
			
			iClient = new VegaANPRClient(iPort,true);
			iClient.addLogEventListener(this);
			iClient.addDataDocumentEventListener(this);
			iClient.start();
			
            log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo init - VegaClient istanziato");
			
			return true;
		}
		catch (Exception vEx) {
			log(VegaANPRLogEventListenerInterface.T_ERR,"Errore metodo init " + vEx.getMessage());
                        vEx.printStackTrace();
		}
		return false;
	}
	
	
	private void initParams() {
		iPort=8888;
	}
	
	
	private void destroy() {
		// Chiusura server socket
		if(iClient != null)
		{
		    iClient.destroy();
		    iClient.interrupt();
		    iClient=null;
		}
	}
	
	
	public void readData(Document aDoc) {
		try {
			log(VegaANPRLogEventListenerInterface.T_DBG,"Metodo readData - Lettura buffer plugin");
			// Alimento i vari TAG
			for(String vKey : iEventList.keySet()) {
				try {
					EventComponent vEvtComp = iEventList.get(vKey);
					if(vEvtComp.getIsMsgRet())
						vEvtComp.setIValue(aDoc);
				}
				catch(Exception vEx) {
					log(VegaANPRLogEventListenerInterface.T_ERR,"Errore metodo readData - " + vEx.getMessage());
					continue;
				}
			}

			// Creo evento - di default crea evento con tutte le variabili
			createEvent();
		}
		catch (Exception vEx) {
			log(VegaANPRLogEventListenerInterface.T_ERR,"Errore metodo readData - " + vEx.getMessage());
		}
	}
	
	
	private synchronized void createEvent() {
		try {
			// Crea SPIIOTEvent
			SPIIoTEvent vEvent = new SPIIoTEvent(iSubId);
			// Alimentazione struttura Event
			for(String vKey : iEventList.keySet()) {
				EventComponent vEvtComp = iEventList.get(vKey);
				// Ritorno solo le variabili non di tipo CMD
				if(vEvtComp.getIsMsgRet())
					vEvent.setData(vKey, vEvtComp.getIValue());
			}
			//invia Evento
			log(VegaANPRLogEventListenerInterface.T_INFO,"invio evento " + vEvent.getDataTable().toString());
			fireEventToSmeup(vEvent);
		}
		catch (Exception vEx) {
			log(VegaANPRLogEventListenerInterface.T_ERR,"Errore metodo createEvent- " + vEx.getMessage());
		}
	}
	
	public static void main(String[] args) {
//
//        IOTSPIVegaANPRPlugin vPlugin = new IOTSPIVegaANPRPlugin();
//        
//        IoTConnectorConf vConf = new IoTConnectorConf();
//        vConf.addData("Port", "8888");
//        vConf.addData("Debug", "1");
//       
//        
//        System.out.println("Hello World");
//        if (vPlugin.postInit(aSez, vConf)) {
//
//            IoTConnectorInput input = new IoTConnectorInput();
//
//            input.addData("STRINGA", "*READ");
//        	
//            for (int i = 0; i < 10; i++) {
//            }
//        } else {
//        	vPlugin.log(VegaANPRLogEventListenerInterface.T_ERR,"Errore metodo main - Plugin non inizializzato");
//        }
    }


	public void menageLogEvent(VegaANPRLogEvent e) {
		this.log(e.getLevel(), e.getMessage());
	}
	

	public void menageEventDataDocument(VegaANPRDataDocumentEvent e) {
		this.readData(e.getDataDocument());
	}

}
