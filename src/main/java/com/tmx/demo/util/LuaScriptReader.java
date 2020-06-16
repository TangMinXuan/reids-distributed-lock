package com.tmx.demo.util;

import org.springframework.core.io.ClassPathResource;

import java.io.*;

public class LuaScriptReader {

    public static StringBuilder read(String scriptName) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(scriptName);
        InputStream is = classPathResource.getInputStream();
        String line;    //用于保存每行的内容
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder res = new StringBuilder();

        line = reader.readLine();
        while(line != null) {
            res.append(line);
            res.append("\n");
            line = reader.readLine();
        }

        reader.close();
        is.close();
        return res;
    }
}
