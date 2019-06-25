package com.smeup.iotspi;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.smeup.iotspi.jd003.Jd003Plugin;

import Smeup.smeui.iotspi.datastructure.interfaces.SezConfInterface;
import Smeup.smeui.iotspi.datastructure.interfaces.SezInterface;
import Smeup.smeui.iotspi.datastructure.interfaces.SubInterface;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorConf;
import Smeup.smeui.iotspi.interfaces.SPIIoTFrameworkInterface;

class Jd003PluginTest extends Thread{
	
	private IoTConnectorConf connectorConf = new IoTConnectorConf();
	private SezInterface sezInterface = getSezInterfaceInstance();
    private Jd003Plugin jd003Plugin = new Jd003Plugin();


	@Test
	void test() {
		connectorConf.addData("Port", "8888");
		connectorConf.addData("RpgSources", "src/test/resources/rpg/");
		
		assertEquals(true, jd003Plugin.postInit(sezInterface, connectorConf));
	}
	
	@Test
	void test_openSocket() {

		// wait the socket is up
//		Thread.sleep(2000);
		
		final String address = "localhost";
		final int port = 8888;
		final String message = "Data send to: " + address +":"+ port;
		
	    Callable<String> callable = new Callable<String>() {
	        @Override
	        public String call() {
	        	connectorConf.addData("Port", "8888");
	    		connectorConf.addData("RpgSources", "src/test/resources/rpg/");
	    		
	    		jd003Plugin.postInit(sezInterface, connectorConf);
	        	
	    		return null;
	        }
	    };
	    
	    ExecutorService executor = Executors.newSingleThreadExecutor();
	    Future<String> future = executor.submit(callable);
	    
	    
	    writeSocket(address, port, message);
	
	    executor.shutdown();
		
	}
	
	
	public void run() {
		
	}
	
	
	private void writeSocket(String address, int port, String message) throws UnknownHostException,IOException{
	
		Socket socket = new Socket(address, port);
		OutputStream output = socket.getOutputStream();
		PrintWriter writer = new PrintWriter(output, true);
		writer.println(message);
		writer.flush();
		writer.close();
		socket.close();
		
	}
	
	private SezInterface getSezInterfaceInstance() {
		return new SezInterface() {
			
			@Override
			public void log(String aMessage) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean init(SPIIoTFrameworkInterface aFramework, String aId, String aName) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public int getType() {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public String getTOgg() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public HashMap<String, SubInterface> getSubTable() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getPluginClass() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getPgm() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getOgg() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getId() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public SezConfInterface getConf() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public boolean checkServerSession() {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}

}
