package com.xiaonan.xnbi.utils.image;

import java.io.*;

public class ImageUtil {
    /**
     * 通过文件路径读取图片
     * @param filePath 文件路径
     * @return
     * @throws IOException
     */
    public static byte[] readByFilePath(String filePath) throws IOException {
        InputStream in = new FileInputStream(filePath);
        byte[] data = inputStream2ByteArray(in);
        in.close();
        return data;
    }
    private static byte[] inputStream2ByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * 通过文件解析
     * @param file 文件
     * @return
     * @throws IOException
     */
    public static byte[] readByFile(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        byte[] data = inputStream2ByteArray(in);
        in.close();
        return data;
    }
}
