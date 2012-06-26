package com.applang.db;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

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

}
