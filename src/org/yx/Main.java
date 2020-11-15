package org.yx;

import org.yx.util.ThreadDownload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36 Edg/83.0.478.61";

    public static void main(String[] args) {

//        downloadByUrl("http://xiongxiaoxiong.online/index.html");
//        downloadByUrl("http://xiongxiaoxiong.online/mysql-installer-community-5.6.42.0.msi");
//        downloadByUrl("https://github.com/git-for-windows/git/releases/download/v2.27.0.windows.1/Git-2.27.0-64-bit.exe");
//        downloadByUrl("http://cdimage.deepin.com/releases/20Beta/deepin-20Beta-desktop-amd64.iso");
        ThreadDownload.downloadByUrl("http://cdimage.deepin.com/releases/20Beta/deepin-20Beta-desktop-amd64.iso");
    }

}
