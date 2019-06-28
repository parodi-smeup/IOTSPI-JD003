package com.smeup.iotspi.jd003;

import java.io.PrintStream;

import com.smeup.jd.JD_NFYEVE;
import com.smeup.rpgparser.interpreter.Program;
import com.smeup.rpgparser.jvminterop.JavaSystemInterface;

public class MyJavaSystemInterface extends JavaSystemInterface {
	public MyJavaSystemInterface(PrintStream printStream) {
		super(printStream);
	}

	@Override
	public Program instantiateProgram(Class<?> arg0) {
		Program program = super.instantiateProgram(arg0);
		if (program instanceof JD_NFYEVE) {
			//DO some stuff
		}
		return program;
	}
}
