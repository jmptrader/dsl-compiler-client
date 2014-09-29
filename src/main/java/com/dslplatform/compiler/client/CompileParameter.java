package com.dslplatform.compiler.client;

public interface CompileParameter {
	public boolean check(final Context context) throws ExitException;

	public void run(final Context context) throws ExitException;

	public String getShortDescription();

	public String getDetailedDescription();
}
