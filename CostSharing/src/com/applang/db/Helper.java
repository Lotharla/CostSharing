package com.applang.db;

import java.lang.reflect.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.*;

import com.applang.share.*;

public class Helper
{
	public static String formatAmount(double value) {
		return String.format("%.2f", value);
	}

	public static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static String timestamp(Date date) {
		return timestampFormat.format(date);
	}
    
    public static String timestampNow() {
		return timestamp(new Date());
	}
 
	public static String packageName() {
		return "com.applang";
	}

	public static String pathToDatabases() {
		return "data/" + packageName() + "/databases";
	}

	public static String databaseName() {
		return "data";
	}

	public static File databasesDir() {
		return new File(new File(getDataDirectory()), pathToDatabases());
	}

	public static File databaseFile() {
		return new File(databasesDir(), databaseName());
	}

	public static String getDataDirectory() {
		String dir = System.getProperty("data.dir");
		if (Util.notNullOrEmpty(dir))
			return dir;
		else
			return System.getProperty("user.dir");
	}

	public static void setDataDirectory(String dir) {
		System.setProperty("data.dir", dir);
	}

	public static <T> String captureOutput(Util.Job job, Object... params) throws Exception {
		PrintStream out = System.out;
		ByteArrayOutputStream myOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(myOut));
		
		job.dispatch(params);
		
		String output = myOut.toString();
		System.setOut(out);
		return output;
	}	
    
	public static class LogFormatter extends Formatter
	{
		@Override
		public String format(LogRecord record) {
			StringBuilder sb = new StringBuilder();
			sb.append(timestampNow());
			sb.append(String.format(" %s : ", record.getLevel()));
			sb.append(formatMessage(record));
			sb.append("\n");
			return sb.toString();
		}
	}

    public static void logFileHandling(String pattern, int limit, int count, boolean append) {
		try {
			Logger rootLogger = Logger.getLogger("");
	    	for (Handler handler : rootLogger.getHandlers()) 
	    		if (handler instanceof ConsoleHandler) {
	    			rootLogger.removeHandler(handler);
	    			break;
	    		}
	    	
			FileHandler fileHandler = new FileHandler(pattern, limit, count, append);
			fileHandler.setFormatter(new LogFormatter());
			rootLogger.addHandler(fileHandler);
		} catch (Exception ex) {
			new ErrorManager().error("logFileHandling", ex, ErrorManager.GENERIC_FAILURE);
		}
    }
	
	public static Set<String> getTestsJUnit3(String testClassName) {
		Set<String> tests = new HashSet<String>();
		
		try {
			Class<?> type = Class.forName(testClassName);
			for (Method m : type.getDeclaredMethods()) {
				String testName = m.getName();
				if (testName.startsWith("test"))
					tests.add(testName);
			}
			for (Method m : type.getSuperclass().getDeclaredMethods()) {
				String testName = m.getName();
				if (testName.startsWith("test"))
					tests.add(testName);
			}
		} catch (Exception e) {}
		
		return tests;
	}
}
