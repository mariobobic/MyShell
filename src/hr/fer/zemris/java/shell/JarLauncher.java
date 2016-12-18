package hr.fer.zemris.java.shell;

import java.awt.GraphicsEnvironment;
import java.io.Console;
import java.io.IOException;

/**
 * Opens the command line application if it is not yet opened and launches
 * MyShell.
 * <p>
 * This is used to open the jar file in command line directly in the explorer.
 *
 * @author Brandon Barajas
 */
// http://stackoverflow.com/questions/7704405/how-do-i-make-my-java-application-open-a-console-terminal-window
public class JarLauncher {
	
	/**
	 * Program entry point.
	 * 
	 * @param args not used
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Console console = System.console();

		if (console == null && !GraphicsEnvironment.isHeadless()) {
			String filename = JarLauncher.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
			Runtime.getRuntime().exec(new String[] {"cmd", "/c", "start", "cmd", "/k", "java -jar \"" + filename + "\""});
		} else {
			MyShell.main(new String[0]);
		}
	}
}