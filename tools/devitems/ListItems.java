import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Prints every concrete Item class (public, non-abstract, public no-arg constructor)
 * found in a compiled SPD class directory, one fully-qualified name per line.
 * Usage: java -cp <deps>:<classes>:. ListItems <classes-dir>
 */
public class ListItems {
	public static void main(String[] args) throws Exception {
		File root = new File(args[0]);
		ClassLoader cl = new URLClassLoader(new URL[]{root.toURI().toURL()}, ListItems.class.getClassLoader());
		Class<?> item = Class.forName("com.shatteredpixel.shatteredpixeldungeon.items.Item", false, cl);
		ArrayList<String> names = new ArrayList<>();
		scan(new File(root, "com/shatteredpixel/shatteredpixeldungeon"), root, names);
		ArrayList<String> out = new ArrayList<>();
		for (String n : names) {
			try {
				Class<?> c = Class.forName(n, false, cl);
				if (!item.isAssignableFrom(c)) continue;
				if (Modifier.isAbstract(c.getModifiers()) || !Modifier.isPublic(c.getModifiers())) continue;
				if (c.getEnclosingClass() != null && !Modifier.isStatic(c.getModifiers())) continue;
				if (c.isAnonymousClass() || c.isLocalClass()) continue;
				if (!Modifier.isPublic(c.getConstructor().getModifiers())) continue;
				out.add(c.getName());
			} catch (Throwable t) {
				// no public no-arg constructor or not loadable: skip
			}
		}
		Collections.sort(out);
		for (String s : out) System.out.println(s);
	}

	static void scan(File dir, File root, ArrayList<String> names) {
		File[] fs = dir.listFiles();
		if (fs == null) return;
		for (File f : fs) {
			if (f.isDirectory()) scan(f, root, names);
			else if (f.getName().endsWith(".class")) {
				String rel = root.toURI().relativize(f.toURI()).getPath();
				names.add(rel.substring(0, rel.length() - 6).replace('/', '.'));
			}
		}
	}
}
