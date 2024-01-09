package com.dbl.service;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.v2.files.DownloadZipResult;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DropBoxUtilsImplTest {


/*
    @Test
    public void downloadZipTest() throws IOException {
        Map<String, byte[]> out = new HashMap<>();

        //Loading the file from filesystem
        String name = "../../../Payments2.zip";
        System.out.println("running on " + name);
        String fullPath = this.getClass().getResource(name).getPath();
        InputStream initialStream = new FileInputStream(new File(fullPath));

        // open a stream from it
        String targetFilePath = fullPath.substring(0, fullPath.lastIndexOf("/")) + "/targetFile.tmp";
        System.out.println("targetFilePath:" + targetFilePath);

        //making a temp file from this stream
        File targetFile = new File(targetFilePath);
        FileUtils.copyInputStreamToFile(initialStream, targetFile);

        //load the temp file
        File sourceFile = new File(targetFilePath);
        FileInputStream in = new FileInputStream(sourceFile);
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(in);

            ZipEntry zipEntry = zis.getNextEntry();
            byte[] buffer = new byte[1024];
            while (zipEntry != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                out.put(zipEntry.getName(), baos.toByteArray());
                baos.close();
                zipEntry = zis.getNextEntry();
            }

        } finally {
            in.close();
            if (zis != null) {
                zis.close();
            }
        }

        System.out.println(out.keySet());

    }
*/
}
