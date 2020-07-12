package org.yx;

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
        downloadByUrl("http://xiongxiaoxiong.online/mysql-installer-community-5.6.42.0.msi");
//        downloadByUrl("https://github.com/git-for-windows/git/releases/download/v2.27.0.windows.1/Git-2.27.0-64-bit.exe");
//        downloadByUrl("http://cdimage.deepin.com/releases/20Beta/deepin-20Beta-desktop-amd64.iso");

    }


    public static void downloadByUrl(String urlString) {

        try {
            URL url = new URL(urlString);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(10000);//设置超时时间
            httpURLConnection.setRequestProperty("Range", "bytes=0-10");
            int responseCode = httpURLConnection.getResponseCode();
            String responseMessage = httpURLConnection.getResponseMessage();

            System.out.println("responseMessage:" + responseMessage);

            if (responseCode == 206) {
                System.out.println("该资源支持多线程下载");
                String headerField = httpURLConnection.getHeaderField("Content-Range");
                String byteSizeString = headerField.substring(headerField.lastIndexOf("/") + 1);
                long byteSize = Long.parseLong(byteSizeString);
                System.out.println(byteSize);
                System.out.println("文件大小：" + fileSizeFormat(byteSize));

                //启动线程池
                int core = Runtime.getRuntime().availableProcessors();

                core = 5;

                ExecutorService executorService = Executors.newFixedThreadPool(core);

                //计算每个线程需要下载的段落
                Long blockSize = byteSize / core;

                for (int i = 0; i < core; i++) {

                    Long startIndex = i * blockSize;//起始位置
                    Long endIndex = (i + 1) * blockSize - 1;//结束位置
                    //当最后一个线程时，计算逻辑改为以总大小-1的位置
                    if (i == core - 1) {
                        endIndex = byteSize - 1;
                    }
                    executorService.submit(new DownloadTask(startIndex, endIndex, urlString));
                }

                executorService.shutdown();

                while (!executorService.isTerminated()) {
                    Thread.sleep(1);
                }

                System.out.println("下载完成");

            } else {
                System.err.println("该资源不支持多线程,responseCode:" + responseCode);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


    /**
     * 编写字节换算方法
     *
     * @param size 字节数byte
     * @return KB，MB，GB
     */
    public static String fileSizeFormat(Long size) {

        BigDecimal fileSize = new BigDecimal(size);

        String result = "b";

        if (size < 1024) {
            BigDecimal bigDecimal = new BigDecimal(size);
            String s = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            return s + result;
        } else if (size >= 1024 && size < 1024 * 1024) {
            BigDecimal kb = new BigDecimal(1024);
            String s = fileSize.divide(kb).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            return s + "K" + result;
        } else if (size >= 1024 * 1024 && size < 1024 * 1024 * 1024) {
            BigDecimal mb = new BigDecimal(1024 * 1024);
            String s = fileSize.divide(mb).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            return s + "M" + result;
        } else {
            BigDecimal gb = new BigDecimal(1024 * 1024 * 1024);
            String s = fileSize.divide(gb).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            return s + "G" + result;
        }

    }


    static class DownloadTask implements Runnable {

        private Long startIndex;
        private Long endIndex;
        private String urlString;

        public DownloadTask(Long startIndex, Long endIndex, String urlString) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.urlString = urlString;
        }

        @Override
        public void run() {
            try {


                URL url = new URL(urlString);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setConnectTimeout(10000);//设置超时时间

                String range = "bytes=" + startIndex + "-" + endIndex;
                httpURLConnection.setRequestProperty("range", range);
                httpURLConnection.setRequestProperty("user-Agent", UA);
                int responseCode = httpURLConnection.getResponseCode();
                String responseMessage = httpURLConnection.getResponseMessage();
                if (responseCode == 206) {
                    System.out.println(Thread.currentThread().getName() + "正在执行下载任务");

                    InputStream inputStream = httpURLConnection.getInputStream();

                    //随机读写
                    RandomAccessFile randomAccessFile = new RandomAccessFile(getDownloadPath(urlString), "rw");
                    //跳转到当前线程需要写入的地址段
                    randomAccessFile.seek(startIndex);

                    byte[] bytes = new byte[1024];
                    int length = 0;

                    while ((length = inputStream.read(bytes)) != -1) {
                        randomAccessFile.write(bytes, 0, length);
                    }

                    randomAccessFile.close();

                    inputStream.close();
                } else {
                    System.err.println(Thread.currentThread().getName() + "\thttp状态不正确,responseMessage:" + responseMessage + ",responseCode:" + responseCode);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }


    public static String getDownloadPath(String urlString) {

        String userHome = System.getProperty("user.home");

        String fileName = urlString.substring(urlString.lastIndexOf("/") + 1);

        String filePath = userHome + "/Desktop/" + fileName;

        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return filePath;


    }


}
