package edu.gdut.MF.core;

import edu.gdut.MF.annotation.MFConfig;
import edu.gdut.MF.exception.MFException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ContextLoaderCore {
    private final List<String> classPath = new ArrayList<>();

    private final BeanFactory beanFactory;

    String outerPath;

    public ContextLoaderCore(Class<?> config) {
        init(config);
        this.beanFactory = new BeanFactory(classPath, config);
    }

    private void init(Class<?> config) {
        if (!config.isAnnotationPresent(MFConfig.class))
            throw new MFException("not config class received!");
        outerPath = Objects.requireNonNull(config.getClassLoader().getResource("")).getPath();
        String path = outerPath + config.getPackage().getName().replace(".", File.separator);
        try {
            Files.walkFileTree(Paths.get(path), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toFile().getName().endsWith(".class")) {
                        classPath.add(file.toFile().getPath()
                                .replaceFirst("^" + outerPath, ""));
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
            throw new MFException("cant scan the file to init!", e);
        }
    }

    public Object get(Class<?> type) {
        return beanFactory.get(type);
    }

    public Object get(String beanName) {
        return beanFactory.get(beanName);
    }

}
