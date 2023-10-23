package com.stardevllc.bootstrap;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public final class StarBootstrapper {
    public static void main(String[] args) {
        Path jarsFolder = Path.of("./jars");
        if (Files.notExists(jarsFolder)) {
            try {
                Files.createDirectory(jarsFolder);
            } catch (IOException e) {
                System.err.println("Could not create the /jars directory: " + e.getMessage());
                return;
            }
        }

        List<URL> jarUrls = new ArrayList<>();
        
        AtomicReference<String> mainClassName = new AtomicReference<>("");

        try (Stream<Path> files = Files.walk(jarsFolder)) {
            files.forEach(path -> {
                String fileName = path.getFileName().toString();
                int extStart = fileName.lastIndexOf(".");
                if (extStart <= 0) {
                    return;
                }
                String extension = fileName.substring(extStart);
                if (extension.equalsIgnoreCase(".jar") || extension.equalsIgnoreCase("jar")) {
                    try {
                        jarUrls.add(path.toUri().toURL());
                        JarInputStream jis = new JarInputStream(new FileInputStream(path.toFile()));
                        Manifest manifest = jis.getManifest();
                        if (manifest != null) {
                            String value = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                            if (!value.isEmpty()) {
                                mainClassName.set(value);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error while searching files: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Error while searching files: " + e.getMessage());
            return;
        }

        StarClassLoader classLoader = new StarClassLoader(jarUrls.toArray(new URL[0]));
        Thread runThread = new Thread(() -> {
            if (!mainClassName.get().isEmpty()) {
                try {
                    Class<?> mainClass = Class.forName(mainClassName.get(), true, classLoader);
                    MethodHandle mainHandle = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(void.class, String[].class));
                    mainHandle.invoke(args);
                } catch (Throwable e) {
                    System.err.println("Could not load main class: " + mainClassName.get());
                }
            }
        });
        
        runThread.setContextClassLoader(classLoader);
        runThread.start();
    }
}