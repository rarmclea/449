package parser;

import java.lang.reflect.*;
import java.text.ParseException;
import java.util.Scanner;

public class Interpreter {
	static boolean verbose;
	static boolean running = true;
	static Class subject;
	
	public static void main(String[] args){
		checkCommandLineArgs(args);
	}
	
	private static void checkCommandLineArgs(String[] args){
		boolean h = false;
		String classname = null;
		String filename = null;
		for (String arg : args){
			if (arg.charAt(0) == '-'){
				if (arg.equals("-v") || arg.equals("--verbose"))
					verbose = true;
				else if ((arg.equals("-?") || arg.equals("-h")) || arg.equals("--help"))
					h = true;
				else if (arg.charAt(1) == '-'){
					System.err.println("Unrecognized qualifier " + arg);
					synopsis();
					System.exit(-1);
				}
				else {
					System.err.println("Unrecognized qualifier '" + arg.charAt(1) + "' in '" + arg + "'");
					synopsis();
					System.exit(-1);
				}
			}
			else if (filename == null){
				filename = arg;
			}
			else if (classname == null){
				classname = arg;
			}
			else {
				System.err.println("This program takes at most two command-line arguments");
				synopsis();
				System.exit(-2);
			}
		}
		if (args.length == 0)
			synopsis();
		else if (h && (filename == null)){
			synopsis();
			System.out.println("\nThis program interprets commands of the format '(<method> {arg}*)' on the command line, finds corresponding");
			System.out.println("methods in <class-name>, and executes them, printing the result to sysout.");
		}
		else if (h && (filename != null)){
			System.err.println("Qualifier '--help' (-h, -?) should not appear with any command-line arguments");
			synopsis();
			System.exit(-4);			
		}
		else {
			try {
				subject = Class.forName(classname);
			} catch (ClassNotFoundException e) {
				System.err.println("Could not find class: " + classname);
				System.exit(-6);
			}
			console();
		}
	}
	
	private static void synopsis(){
		System.out.println("Synopsis:");
		System.out.println("  methods");
		System.out.println("  methods { -h | -? | --help }+");
		System.out.println("  methods {-v --verbose}* <jar-file> [<class-name>]");
		System.out.println("Arguments:");
		System.out.println("  <jar-file>:   The .jar file that contains the class to load (see next line).");
		System.out.println("  <class-name>: The fully qualified class name containing public static command methods to call. [Default=\"Commands\"]");
		System.out.println("Qualifiers:");
		System.out.println("  -v --verbose: Print out detailed errors, warning, and tracking.");
		System.out.println("  -h -? --help: Print out a detailed help message.");
		System.out.println("Single-char qualifiers may be grouped; long qualifiers may be truncated to unique prefixes and are not case sensitive.");

	}
	
	private static void console(){
		Scanner s = new Scanner(System.in);
		help();
		String line;
		while (running) {
			System.out.print("> ");
			line = s.nextLine();
			if (line.length() == 0)
				continue;
			char c = line.charAt(0);
			if (line.length() == 1){
				switch(c){
				case 'q':
					running = false;
					break;
				case '?':
					help();
					break;
				case 'v':
					toggleVerbose();
					break;
				case 'f':
					listFunctions(subject);
					break;	
				default: 
					valueHandler(line);
					break;
				}
			}
			else if (c == '(')
				functionHandler(line.substring(1, (line.length()-1)));
			else{
				valueHandler(line);
			}
		}
		s.close();
		System.out.println("Bye");
	}
	
	private static void toggleVerbose(){
		verbose = !verbose;
		String message = verbose ? "Verbose on" : "Verbose off";
		System.out.println(message);
	}
	
	private static void valueHandler(String input){
		try {
			if (input.charAt(0) == '"'){
				if (input.charAt(input.length()-1) == '"')
					System.out.println(input);
				else
					throw new ParseException("Reached end of string while parsing", input.length());
			}
			else {
				int point = 0;
				for (int i = 0; i < input.length(); i++){
					if (Character.isDigit(input.charAt(i)))
						continue;
					else if (input.charAt(i) == '.')
						point++;
					else
						throw new ParseException("Unexpected character encountered at offset " + i, i);
					
				}
				if (point > 1){
					int offset = input.indexOf('.', (input.indexOf('.')+1));
					throw new ParseException("Unexpected character encountered at offset " + offset, offset);
				}
				else
					System.out.println(input);
			}
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println(input);
			for (int i = 0; i < e.getErrorOffset(); i++)
				System.out.print("-");
			System.out.println("^");
			if (verbose)
				e.printStackTrace(); 
		}
		
	}
	
	private static void functionHandler(String input){
		System.out.println("Handling " + input + "...");
	}
	
	
	private static void listFunctions(Class c){
		Method[] methods = c.getMethods();
		for (int i = 0; i < methods.length; i++){
			System.out.print("(");
			System.out.print(methods[i].getName());
			Parameter[] parameters = methods[i].getParameters();
			for (int j = 0; j < parameters.length; j++){
				System.out.print(" " + parameters[j].getType().getName());
			}
			System.out.print(") : ");
			System.out.println(methods[i].getReturnType().getName());
			
		}
	}
	
	private static void help(){
		System.out.println("q           : Quit the program.");
		System.out.println("v           : Toggle verbose mode (stack traces).");
		System.out.println("f           : List all known functions.");
		System.out.println("?           : Print this helpful text.");
		System.out.println("<expression>: Evaluate the expression.");
		System.out.println("Expressions can be integers, floats, strings (surrounded in double quotes) or function \n calls of the form '(identifier {expression}*)'.);");
	}
}
