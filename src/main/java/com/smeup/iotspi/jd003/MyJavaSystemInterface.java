package com.smeup.iotspi.jd003;

import java.io.PrintStream;

import com.smeup.jd.JD_NFYEVE;
import com.smeup.rpgparser.interpreter.Program;
import com.smeup.rpgparser.jvminterop.JavaSystemInterface;

import Smeup.smeui.iotspi.interaction.SPIIoTConnectorAdapter;

public class MyJavaSystemInterface extends JavaSystemInterface {
	public MyJavaSystemInterface(PrintStream printStream) {
		super(printStream);
	}

	public Program instantiateProgram(Class<?> arg0, SPIIoTConnectorAdapter sPIIoTConnectorAdapter) {
		Program program = super.instantiateProgram(arg0);
		if (program instanceof JD_NFYEVE) {
			((JD_NFYEVE) program).setsPIIoTConnectorAdapter(sPIIoTConnectorAdapter);
		}
		return program;
	}
}
