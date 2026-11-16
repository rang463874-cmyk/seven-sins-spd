package com.shatteredpixel.shatteredpixeldungeon.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.watabou.noosa.Game;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Seven Sins: keeps the stack trace of the last crash (any uncaught exception, on any thread) in a local file,
 * so the title screen can show it on the next launch (TitleScene -> WndCrashLog) and the owner can send it in.
 */
public class CrashLog {

	private static final String FILE = "crash_log.txt";
	private static boolean installed = false;

	public static synchronized void install() {
		if (installed) return;
		installed = true;
		final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				write(t, e);
				if (previous != null) previous.uncaughtException(t, e);
			}
		});
	}

	public static void write(Thread t, Throwable e) {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			pw.println("Seven Sins " + Game.version + " (" + Game.versionCode + ")");
			pw.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date())
					+ "  thread: " + (t == null ? "?" : t.getName()));
			pw.println();
			e.printStackTrace(pw);
			pw.flush();
			file().writeString(sw.toString(), false, "UTF-8");
		} catch (Throwable ignored) {
			//never let the crash log itself get in the way of the crash
		}
	}

	/** the saved crash report, or null if there is none */
	public static String pending() {
		try {
			FileHandle f = file();
			if (f != null && f.exists()) return f.readString("UTF-8");
		} catch (Throwable ignored) {
		}
		return null;
	}

	public static void clear() {
		try {
			FileHandle f = file();
			if (f != null && f.exists()) f.delete();
		} catch (Throwable ignored) {
		}
	}

	private static FileHandle file() {
		return Gdx.files == null ? null : Gdx.files.local(FILE);
	}

	/** the report shortened for the in-game font: short package names, no "_" (the font's highlight marker) */
	public static String forDisplay(String log) {
		StringBuilder out = new StringBuilder();
		int lines = 0;
		for (String line : log.split("\n")) {
			line = line.replace("\t", "  ")
					.replace("com.shatteredpixel.shatteredpixeldungeon.", "")
					.replace("com.watabou.", "")
					.replace("com.badlogic.gdx.", "gdx.")
					.replace('_', '-');
			if (line.trim().startsWith("at android.") || line.trim().startsWith("at java.lang.reflect")) continue;
			out.append(line).append('\n');
			if (++lines >= 60) {
				out.append("...");
				break;
			}
		}
		return out.toString();
	}
}
