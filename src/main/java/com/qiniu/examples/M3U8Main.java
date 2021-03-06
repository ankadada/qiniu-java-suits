package com.qiniu.examples;

import com.qiniu.common.QiniuAuth;
import com.qiniu.common.FileReaderAndWriterMap;
import com.qiniu.common.QiniuException;
import com.qiniu.common.QiniuSuitsException;
import com.qiniu.config.PropertyConfig;
import com.qiniu.service.auvideo.M3U8Manager;
import com.qiniu.service.auvideo.M3U8Tools;
import com.qiniu.service.auvideo.VideoTS;
import com.qiniu.service.oss.AsyncFetch;
import java.io.IOException;
import java.util.List;

public class M3U8Main {

    public static void main(String[] args) throws Exception {

        PropertyConfig propertyConfig = new PropertyConfig(".qiniu.properties");
        String ak = propertyConfig.getProperty("access_key");
        String sk = propertyConfig.getProperty("secret_key");
        String bucket = propertyConfig.getProperty("bucket");
        String targetFileDir = System.getProperty("user.home") + "/Documents/test";
        String url = "http://oi62kxphl.cvoda.com/bWVueWFlcjpsaENSUTVjeUppYWdZQnc5ZGducFZsdTRMN1c4_5862081a64703c8d0300021e.m3u8";
        QiniuAuth auth = QiniuAuth.create(ak, sk);
        M3U8Main m3u8Main = new M3U8Main();

        try {
            m3u8Main.m3u8Download(url, targetFileDir);
            m3u8Main.m3u8Fetch(auth, bucket, url, targetFileDir);
        } catch (Exception ioException) {
            System.out.println("failed: " + ioException.getMessage() + ". it need retry.");
            ioException.printStackTrace();
        }
    }

    public void m3u8Download(String url, String targetFileDir) throws IOException {

        FileReaderAndWriterMap targetFileReaderAndWriterMap = new FileReaderAndWriterMap();
        // writer 和 reader 对象一定要先 init
        targetFileReaderAndWriterMap.initWriter(targetFileDir, "m3u8_down");
        M3U8Tools m3u8Tools = new M3U8Tools(targetFileReaderAndWriterMap);
        M3U8Manager m3u8Manager = new M3U8Manager();
        List<VideoTS> tsList = null;
        System.out.println("get index for: " + url);

        try {
            tsList = m3u8Manager.getVideoTSListByUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        float f = 0;

        for (VideoTS ts : tsList) {
            f += ts.getSeconds();
        }

        System.out.println("movie length: " + ((int) f / 60) + "min" + (int) f % 60 + "sec");
        m3u8Tools.download(tsList, targetFileDir);
        System.out.println("Wait for download...");
        m3u8Tools.merge(tsList, url, targetFileDir);
        targetFileReaderAndWriterMap.closeWriter();
        System.out.println("download completed for: " + targetFileDir);

    }

    public void m3u8Fetch(QiniuAuth auth, String targetBucket, String url, String targetFileDir) throws QiniuSuitsException, IOException {

        FileReaderAndWriterMap targetFileReaderAndWriterMap = new FileReaderAndWriterMap();
        targetFileReaderAndWriterMap.initWriter(targetFileDir, "m3u8_fetch");
        M3U8Manager m3u8Manager = new M3U8Manager();
        AsyncFetch asyncFetch = AsyncFetch.getInstance(auth, targetBucket);
        List<VideoTS> tsList = null;
        String tsUrl;
        System.out.println("m3u8 fetch started...");

        try {
            tsList = m3u8Manager.getVideoTSListByUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (VideoTS ts : tsList) {
            tsUrl = ts.getUrl();

            try {
                asyncFetch.run(tsUrl, tsUrl.substring(tsUrl.indexOf("/", 8) + 1), 0);
                targetFileReaderAndWriterMap.writeSuccess(tsUrl);
            } catch (QiniuException e) {
                targetFileReaderAndWriterMap.writeErrorOrNull(e.error() + "\t" + tsUrl);
            }
        }

        System.out.println("m3u8 fetch finished...");
    }
}