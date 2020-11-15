package org.yx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.util.concurrent.Executors;

public class ThreadDownload {

    private static final int core = Runtime.getRuntime().availableProcessors();

    private static final Logger log = LoggerFactory.getLogger(ThreadDownload.class);


    public static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36 Edg/83.0.478.61";


    public static void downloadByUrl(String urlString) {

        java.util.concurrent.ExecutorService executorService = Executors.newFixedThreadPool(core);

        try {
            URL url = new URL(urlString);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(10000);//设置超时时间
            httpURLConnection.setRequestProperty("Range", "bytes=0-10");
            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode == 206) {
                log.info("该资源支持多线程下载");
                String headerField = httpURLConnection.getHeaderField("Content-Range");
                String byteSizeString = headerField.substring(headerField.lastIndexOf("/") + 1);
                long byteSize = Long.parseLong(byteSizeString);
                log.info("文件大小：{}", fileSizeFormat(byteSize));

                //计算每个线程需要下载的段落
                long blockSize = byteSize / core;

                for (int i = 0; i < core; i++) {

                    Long startIndex = i * blockSize;//起始位置
                    long endIndex = (i + 1) * blockSize - 1;//结束位置
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
                log.info("下载完成");
            } else {
                log.error("该资源不支持多线程,responseCode:{},将使用单线程下载", responseCode);
                // 使用单线程下载
                String contentLength = httpURLConnection.getHeaderField("Content-Length");
                executorService.submit(new DownloadTask(0L, Long.parseLong(contentLength), urlString));
                executorService.shutdown();
                while (!executorService.isTerminated()) {
                    Thread.sleep(1);
                }
                log.info("下载完成");
            }

        } catch (MalformedURLException e) {
            log.error("不正确的URL异常", e);
        } catch (IOException e) {
            log.error("IO异常", e);
        } catch (InterruptedException e) {
            log.error("线程中断异常", e);
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
                    log.info("线程[{}]正在执行下载任务", Thread.currentThread().getName());
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
                    log.error("http状态不正确,responseMessage:{},responseCode:{}", responseMessage, responseCode);
                }
            } catch (Exception e) {
                log.error("下载错误", e);
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
