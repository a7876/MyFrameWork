package edu.gdut.MF.core;

import edu.gdut.MF.exception.MFException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ResourceResolver {
    enum ResourceType implements Predicate<String> {
        CLASS {
            @Override
            public boolean test(String s) {
                // 匿名内部类不用
                return !s.contains("$") && s.endsWith(".class");
            }
        };

        @Override
        public abstract boolean test(String s);
    }

    enum ResourceMapper implements Function<String, String> {
        GET_CLASS {
            @Override
            public String apply(String s) {
                return s.substring(0, s.length() - 6).replace(File.separator, ".");
            }
        };

        @Override
        public abstract String apply(String s);
    }

    private final ArrayList<String> list = new ArrayList<>();

    public List<String> getResultList() {
        return list;
    }

    private String classpath;

    private final Class<?> baseClass;

    public ResourceResolver(Class<?> baseClass) {
        this.baseClass = baseClass;
    }

    public void scan(String basePackage, Predicate<String> predicate, Function<String, String> map) {
        list.clear();
        URL location;
        if (basePackage.contains("edu.gdut.MF")) // 将核心扫入，这样在外部引用本jar时也能使用core中定义的bean
            location = CoreInnerConfig.class.getProtectionDomain().getCodeSource().getLocation();// 获取绝对路径
        else
            location = baseClass.getProtectionDomain().getCodeSource().getLocation();// 获取绝对路径
        classpath = location.getPath();
        basePackage = basePackage.replace(".", File.separator);
        try {
            File file = new File(location.toURI());
            if (file.getName().endsWith("jar")) {
                try (JarFile jarFile = new JarFile(file)) {
                    scanJar(jarFile, basePackage, predicate, map);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                scanFile(file, basePackage, predicate, map);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void scanJar(JarFile jarFile, String basePackage, Predicate<String> predicate, Function<String, String> map) { // 扫描jar
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().getName();
            if (!entryName.startsWith(basePackage))
                continue;
            if (predicate.test(entryName)) {
                // jar里面直接拿到的名字就是可以用于加载的引用名
                list.add(map.apply(entryName));
            }
        }
    }

    private void scanFile(File file, String basePackage, Predicate<String> predicate, Function<String, String> map) {
        String path = file.getPath() + File.separator + basePackage;
        String basePath = file.toString();
        try {
            Files.walkFileTree(Paths.get(path), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.toString();
                    fileName = fileName.substring(classpath.length());
                    if (predicate.test(fileName)) {
                        list.add(map.apply(fileName));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new MFException("cant scan the file", e);
        }
    }
}
