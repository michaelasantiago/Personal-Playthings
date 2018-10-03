import java.io.*;
import java.util.*;

public class style
{
	BufferedReader reader;
	ArrayList<String> lines;
	boolean isJava;
	
	// When running from command line, provide the address of the code to parse as the first argument
	// Not foolproof, not anywhere near as smart as a compiler
	// There will be false positives and there will be a few rare style errors that can slip through
	// Try running this on its own source to see its effectiveness.  Many style errors are present.
	public static void main(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			System.out.println("We need an address, you fool.");
			return;
		}
		new style(args[0], false);
	}
	
	public style(String fileName, boolean java) throws Exception
	{
		isJava = java;
		setRead(new File(fileName));
		lines = new ArrayList<String>(1);
		// Transferring contents of file into a list of lines
		while (true)
		{
			String next;
			next = reader.readLine();
			if (next == null)
				break;
			lines.add(next);
		}
		String curLine = "", preLine, nextLine;
		boolean stringMode = false;
		// Scanning and outputting questionable situations
		for (int i = 0; i < lines.size(); i++)
		{
			preLine = curLine;
			curLine = lines.get(i);
			char preChar = 'X';
			char curChar = 'X';
			char nextChar = 'X';
			for (int j = 0; j < curLine.length(); j++)
			{
				String errorType = null;
				preChar = curChar;
				if (j < curLine.length() - 1)
					nextChar = curLine.charAt(j+1);
				else
					nextChar = '\n';
				curChar = curLine.charAt(j);
				// Breaking early for good comments
				if (!stringMode && curChar == '/')
				{
					if (nextChar == '/' && (curLine.charAt(j+2) == ' ' || curLine.charAt(j+2) == '\t'))
						break;
				}
				// We don't care about characters that are characters
				if (preChar == '\'' && nextChar == '\'')
					continue;
				// We don't care about characters inside strings
				if (curChar == '"' && !stringMode)
					stringMode = true;
				else if (curChar == '"' && preChar != '\\')
					stringMode = false;
				if (stringMode)
					continue;
				String sub;
				boolean commentator = false;
				switch (curChar)
				{
					case '*':
						// If importing an entire library
						if (nextChar == ';' && isJava)
							break;
						// If creating a pointer
						sub = safeSub(curLine,j-4,j);
						if (sub.equals("int ") || sub.equals("har "))
							break;
						// If changing a pointer (starts the line)
						if (j == 1 || preChar == '\t')
							break;
						// If preceded by a parenthesis, implying that it is used in a comparison
						if (preChar == '(')
							break;
						// If referring to a pointer in an assignment or equation
						sub = safeSub(curLine,j-2,j);
						if (sub.equals("= "))
							break;
						// If referring to a pointer in a comparison
						if (sub.equals("> ") || sub.equals("< "))
							break;
					case '+':
						// Accounting for ++
						if (curChar == '+' && nextChar == '+')
						{
							//System.out.println("Increment detected! "+preChar+curChar+nextChar+" ->" + curLine);
							j++;
							break;
						}
					case '-':
						// Accounting for --
						if (curChar == '-' && nextChar == '-')
						{
							//System.out.println("Increment detected! "+preChar+curChar+nextChar+" ->" + curLine);
							j++;
							break;
						}
					case '/':
						// Disregarding comments
						if (curChar == '/' && nextChar == '/')
							break;
						// Finding bad comments
						if (curChar == '/' && preChar == '/' && nextChar != ' ')
						{
							errorType = "Comment missing gratuitous space \t";
							commentator = true;
							break;
						}
							
					case '%':
						// If part of an assignment operation
						if (nextChar == '=')
						{
							//System.out.println("Assignment detected! ->" + curLine);
							j++;
							break;
						}
						// Must be surrounded by spaces
						if ((preChar != ' ' || nextChar != ' '))
							errorType = "Possible arithmetic spacing error with ";
						break;
					case '=':
						// Accounting for !=
						if (preChar == '!' && nextChar == ' ')
							break;
						// Accounting for ==
						if (preChar == ' ' && nextChar == '=')
						{
							j++;
							break;
						}
					case '>':
					case '<':
						// Accounting for #include 
						if ((safeSub(curLine,0,9)).equals("#include "))
							break;
						// Accounting for >=, ==, <=
						if (preChar == ' ' && nextChar == '=')
						{
							j++;
							break;
						}
					
						if (preChar != ' ' || nextChar != ' ')
							errorType = "Possible comparison spacing error with ";
					case ';':
						// Looking for extraneous semicolons
						if (preChar == ';' || nextChar == ';')
							errorType = "Possible extraneous character with ";
						break;
					// Finding if statements that don't enclose with tabs
					case '\t':
						if (nextChar != '\t' && preLine.length() > j)
						{
							// We're at the last tab of the line
							// At the same spot in the last line, we need a {, \t, or "case" | "defa"ult
							char legal = preLine.charAt(j);
							String justInCase = safeSub(preLine,j,j+4);
							if ((legal != '\t' && legal != '{')
							&& !(justInCase.equals("case") || justInCase.equals("defa")))
							{
								errorType = "Possible unenclosed if contents or tab error";
							}
							
						}
						break;
				}
				if (errorType != null)
				{
					char printChar = curChar;
					if (printChar == '\t')
						printChar = ' ';
					String printLn = stripStartTabs(curLine);
					System.out.println(errorType + printChar+"\t:\t"+printLn+"\tLn ("+i+") ("+j+")");
				}
				if (commentator)
				{
					break;
				}
			}
		}
	}

	private void setRead(File readFile) throws Exception
	{
		try
		{
			FileReader f = new FileReader(readFile);
			reader = new BufferedReader(f);
		}
		catch (FileNotFoundException e)
		{
			throw new Exception("Arr, matey.  There be no file by the name \""+readFile.getName()+"\"");
		}
	}
	public static String safeSub(String input, int start, int end)
	{
		if (start < 0)
			start = 0;
		if (end >= input.length())
			end = input.length();
		return input.substring(start,end);
	}
	
	// Removes tabs from the start of a string
	public static String stripStartTabs(String input)
	{
		int tabCount = 0;
		for (int i = 0; i < input.length(); i++)
		{
			if (input.charAt(i) == '\t')
				tabCount++;
			else
				break;
		}
		return safeSub(input,tabCount,input.length());
	}
}
