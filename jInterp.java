import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class jInterp {
	public static JavaCompiler jc;
	public static List<String> options;
	public static int counter;
	public static URLClassLoader ld;

	public static void main(String[] args) throws MalformedURLException,
			SecurityException, IllegalArgumentException, NoSuchMethodException,
			ClassNotFoundException, IllegalAccessException,
			InvocationTargetException {
		init();
		System.err.close();

		Scanner s = new Scanner(System.in);
		String input;
		System.out.print("> ");
		do {

			input = s.nextLine();
			if (input.startsWith("print")) {
				input = input.replaceAll("^print ", "System.out.println(")
						.replaceAll(";$", ");");
			}
			String name = "Interp_" + counter;
			URI stringuri = URI.create("string://tmp/" + name.replace('.', '/')
					+ Kind.SOURCE.extension);

			if (compile(name, stringuri, input, false)) {
				counter++;
			} else if (compile(name, stringuri, input, true)) {
				counter++;
				run(name);
			} else {
				System.out.println("That's not java..");
			}
			System.out.print("> ");

		} while (s.hasNext());
	}

	public static void init() throws MalformedURLException {
		jc = ToolProvider.getSystemJavaCompiler();
		options = Arrays.asList(new String[] { "-d", "tmp", "-cp", "tmp" });
		counter = 0;
		URL[] urls;
		File f = new File("tmp");
		if (!f.exists())
			f.mkdir();
		urls = new URL[] { f.toURI().toURL() };
		ld = new URLClassLoader(urls);
	}

	public static boolean compile(String name, URI uri, String line,
			boolean inMethod) {
		StringBuffer code = new StringBuffer();
		code.append("import java.io.*;\nimport java.util.*;\npublic class "
				+ name);
		if (counter > 0)
			code.append(" extends Interp_" + (counter - 1) + " ");
		code.append("{\n");
		if (!inMethod)
			code.append("public static " + line + "\n");
		code.append("public static void exec() {\n");
		if (inMethod)
			code.append(line + "\n");
		code.append("}\n}\n");

		JavaFileObject file = new JavaSourceFromString(uri, new String(code));
		Iterable<? extends JavaFileObject> compilationUnits = Arrays
				.asList(file);
		CompilationTask task = jc.getTask(null, null, null, options, null,
				compilationUnits);

		return task.call();
	}

	public static void run(String name) throws SecurityException,
			NoSuchMethodException, ClassNotFoundException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		Method m = Class.forName(name, true, ld).getMethod("exec",
				(Class<?>[]) null);

		m.invoke(null, (Object[]) null);
		System.out.println("\n\n");
	}
}

class JavaSourceFromString extends SimpleJavaFileObject {
	final String code;

	JavaSourceFromString(URI uri, String code) {
		super(uri, Kind.SOURCE);
		this.code = code;
	}

	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return code;
	}
}