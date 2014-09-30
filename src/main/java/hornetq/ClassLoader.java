package hornetq;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ClassLoader extends java.lang.ClassLoader {

    private Map<String, List<byte[]>> index;

    public ClassLoader(java.lang.ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException cnfe) {
                    c = getParent().loadClass(name);
                }
            }
            if (true) {
                resolveClass(c);
            }
            return c;
        }
    }

    private URL selfLocation() {
        Class<? extends ClassLoader> selfClass = getClass();
        ProtectionDomain selfProtectionDomain = selfClass.getProtectionDomain();
        CodeSource selfCodeSource = selfProtectionDomain.getCodeSource();
        return selfCodeSource.getLocation();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (index == null) {
            synchronized (this) {
                if (index == null) {
                    createindex();
                }
            }
        }

        String clsFile = name.replace('.', '/') + ".class";
        List<byte[]> list = index.get(clsFile);
        if (list == null)
            throw new ClassNotFoundException();
        byte[] bs = list.get(0);
        if (bs == null)
            throw new ClassNotFoundException();

        String pkgName = name.substring(0, name.lastIndexOf('.'));
        if (getPackage(pkgName) == null)
            definePackage(name.substring(0, name.lastIndexOf('.')), null, null,
                    null, null, null, null, null);
        return defineClass(name, bs, 0, bs.length);
    }

    private void createindex() {
        index = new HashMap<>();
        try {
            File file = new File(selfLocation().toURI());

            try (FileInputStream fis = new FileInputStream(file);
                    JarInputStream jis = new JarInputStream(fis, true)) {
                JarEntry jarEntry;
                while ((jarEntry = jis.getNextJarEntry()) != null) {
                    if (jarEntry.isDirectory())
                        continue;

                    if (jarEntry.getName().endsWith(".jar")
                            && jarEntry.getName().startsWith("bootjar/")) {
                        try (JarInputStream innerJis = new JarInputStream(
                                new InputStream() {

                                    @Override
                                    public int read() throws IOException {
                                        return jis.read();
                                    }
                                })) {
                            JarEntry innerJarEntry;
                            while ((innerJarEntry = innerJis.getNextJarEntry()) != null) {
                                storeEntry(innerJarEntry.getName(),
                                        readEntry(innerJis));
                            }
                        }
                        continue;
                    }
                    storeEntry(jarEntry.getName(), readEntry(jis));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (URISyntaxException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void storeEntry(String name, byte[] entry) {
        List<byte[]> list = index.get(name);
        if (list == null) {
            list = new ArrayList<byte[]>();
            index.put(name, list);
        }
        list.add(entry);
    }

    private byte[] readEntry(InputStream is) throws IOException {
        int read = 0;
        int remaing = 0;
        int sum = 0;
        byte[] cp = new byte[4096];
        while (read != -1) {
            remaing = cp.length - sum;
            while (remaing > 0 && (read = is.read(cp, sum, remaing)) != -1) {
                remaing -= read;
                sum += read;
            }
            byte[] next = new byte[cp.length + 4096];
            System.arraycopy(cp, 0, next, 0, cp.length - remaing);
            cp = next;
        }
        byte[] res = new byte[sum];
        System.arraycopy(cp, 0, res, 0, sum);
        return res;
    }
}
