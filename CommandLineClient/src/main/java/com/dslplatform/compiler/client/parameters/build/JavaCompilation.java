package com.dslplatform.compiler.client.parameters.build;

import com.dslplatform.compiler.client.Context;
import com.dslplatform.compiler.client.Either;
import com.dslplatform.compiler.client.Utils;
import com.dslplatform.compiler.client.parameters.JavaPath;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

class JavaCompilation {

	static Either<String> compile(
			final String name,
			final File libraries,
			final File source,
			final File output,
			final Map<String, List<String>> services,
			final Context context) {
		if (output.exists() && !output.isDirectory()) {
			if (!output.delete()) {
				return Either.fail("Failed to remove previous Java model: " + output.getAbsolutePath());
			}
		} else if (output.exists() && output.isDirectory()) {
			return Either.fail("Expecting to find file. Found folder at: " + output.getAbsolutePath());
		}
		if (output.getParentFile() != null && !output.getParentFile().exists()) {
			context.show("Output folder not found. Will create one in: " + output.getParent());
			if (!output.getParentFile().mkdirs()) {
				return Either.fail("Unable to create output folder for: " + output.getAbsolutePath());
			}
		}
		final Either<String> tryCompiler = JavaPath.findCompiler(context);
		if (!tryCompiler.isSuccess()) {
			return Either.fail(tryCompiler.whyNot());
		}
		final String javac = tryCompiler.get();
		final File classOut = new File(source, "compile-" + name);
		if (classOut.exists() && !classOut.delete()) {
			return Either.fail("Can't remove folder with compiled files: " + classOut.getAbsolutePath());
		}
		if (!classOut.mkdirs()) {
			return Either.fail("Error creating temporary folder for Java class files: " + classOut.getAbsolutePath());
		}
		final File[] externalJars = libraries.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.toLowerCase().endsWith(".jar");
			}
		});
		if (externalJars.length == 0) {
			return Either.fail("Unable to find dependencies in: " + libraries.getAbsolutePath());
		}

		final List<String> javacArguments = new ArrayList<String>();
		javacArguments.add("-encoding");
		javacArguments.add("UTF-8");
		javacArguments.add("-Xlint:none"); // notices still get emitted to the stderr
		javacArguments.add("-d");
		javacArguments.add("compile-" + name);
		javacArguments.add("-cp");
		final StringBuilder classPath = new StringBuilder(".");
		for (final File j : externalJars) {
			classPath.append(File.pathSeparatorChar).append(j.getAbsolutePath());
		}
		javacArguments.add(classPath.toString());
		context.notify("JAVAC", javacArguments);
		List<String> sources = Utils.listSources(source, context, ".java");
		if (sources.isEmpty())
			return Either.fail("Unable to find Java generated sources in: " + source.getAbsolutePath());
		javacArguments.addAll(sources);
		context.show("Running javac for " + output.getName() + " ...");
		final Either<Utils.CommandResult> execCompile = Utils.runCommand(context, javac, source, javacArguments);
		if (!execCompile.isSuccess()) {
			return Either.fail(execCompile.whyNot());
		}
		final Utils.CommandResult compilation = execCompile.get();
		if (compilation.exitCode != 0) {
			if (compilation.error.length() > 0) {
				return Either.fail(compilation.error);
			} else {
				return Either.fail("Non-zero exit code: " + compilation.exitCode);
			}
		}
		if (compilation.output.contains("error")) {
			final StringBuilder sb = new StringBuilder();
			for (final String e : compilation.output.split("\n")) {
				if (e.contains("error")) {
					sb.append(e).append("\n");
				}
			}
			if (sb.length() > 0) {
				return Either.fail(sb.toString());
			}
			return Either.fail(compilation.output);
		}

		final Either<Utils.CommandResult> tryArchive = JavaPath.makeArchive(context, classOut, output, services);
		if (!tryArchive.isSuccess()) {
			return Either.fail(tryArchive.whyNot());
		}
		return Either.success(compilation.output);
	}
}
