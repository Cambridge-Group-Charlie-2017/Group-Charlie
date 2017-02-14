package uk.ac.cam.cl.charlie.util;

import java.io.File;

/**
 * Utility class for OS relevant information
 *
 * @author Gary Guo
 */
public class OS {
	/**
	 * Enum type of OS
	 */
	public enum Type {
		Linux, Windows, MacOS, Unknown
	}

    private static Type osType;

	/**
	 * Get the enum value representing the current OS.
	 *
	 * @return enum value representing the current OS
	 */
	public static Type getType() {
		if (osType == null) {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.startsWith("windows"))
				osType = Type.Windows;
			else if (os.startsWith("linux"))
				osType = Type.Linux;
			else if (os.startsWith("mac"))
				osType = Type.MacOS;
			else
				osType = Type.Unknown;
		}
		return osType;
	}

	/**
	 * Check if the current OS is Windows.
	 *
	 * @return true if the current OS is Windows, false if not
	 */
	public static final boolean isWindows() {
		return getType() == Type.Windows;
	}

	/**
	 * Check if the current OS is MacOS.
	 *
	 * @return true if the current OS is MacOS, false if not
	 */
	public static final boolean isMacOS() {
		return getType() == Type.MacOS;
	}

	/**
	 * Check if the current OS is Linux.
	 *
	 * @return true if the current OS is Linux, false if not
	 */
	public static final boolean isLinux() {
		return getType() == Type.Linux;
	}

	/**
	 * Get the current OS's top-level application data directory.
	 *
	 * @return path to the top-level application data directory
	 */
	public static String getAppDataDirectory() {
		String path;
		if (isWindows()) {
			path = System.getenv("LocalAppData");
		} else {
			path = System.getProperty("user.home");
			if (isMacOS()) {
				path += "/Library/Application Support";
			}
		}
		return path;
	}

	/**
	 * Get the application data directory for the given name in the current OS.
	 *
	 * @param appName
	 *            name of the application
	 * @return path to the application data directory
	 */
	public static String getAppDataDirectory(String appName) {
		if (!isWindows()) {
			appName = "." + appName;
		}
		return getAppDataDirectory() + File.separator + appName;
	}
}
