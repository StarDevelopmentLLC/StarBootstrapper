package com.stardevllc.bootstrap;

import java.net.URL;
import java.net.URLClassLoader;

public class StarClassLoader extends URLClassLoader {
    public StarClassLoader(URL[] urls) {
        super(urls);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
