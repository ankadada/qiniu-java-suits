package com.qiniu.examples;

import com.qiniu.common.QiniuException;
import com.qiniu.common.QiniuSuitsException;
import com.qiniu.common.FileReaderAndWriterMap;
import com.qiniu.common.QiniuAuth;
import com.qiniu.config.PropertyConfig;
import com.qiniu.interfaces.ILineParser;
import com.qiniu.service.fileline.SplitLineParser;
import com.qiniu.service.oss.AsyncFetch;

import java.io.BufferedReader;
import java.io.IOException;

public class FetchMain {

    public static void main(String[] args) throws Exception {

        PropertyConfig propertyConfig = new PropertyConfig(".qiniu.properties");
        String ak = propertyConfig.getProperty("access_key");
        String sk = propertyConfig.getProperty("secret_key");
        String bucket = propertyConfig.getProperty("bucket");
        String targetFileDir = System.getProperty("user.home") + "/Works/myaccount";
        String sourceFileDir = System.getProperty("user.home") + "/Works/myaccount";
        String[] sourceReaders = new String[]{"xaa", "xab", "xac", "xad", "xae", "xaf", "xag", "xah", "xai", "xaj", "xak", "xal"};

        FileReaderAndWriterMap targetFileReaderAndWriterMap = new FileReaderAndWriterMap();
        AsyncFetch asyncFetch = AsyncFetch.getInstance(QiniuAuth.create(ak, sk), bucket);
        String str;

        try {
            targetFileReaderAndWriterMap.initWriter(targetFileDir, "fetch");
            targetFileReaderAndWriterMap.initReader(sourceFileDir);
            System.out.println("fetch started...");
            BufferedReader bufferedReader = null;
            String fetchResult;
            ILineParser lineParser;

            for (String sourceReaderKey : sourceReaders) {

                try {
                    bufferedReader = targetFileReaderAndWriterMap.getReader(sourceReaderKey);
                } catch (NullPointerException nullPointerException) {
                    nullPointerException.printStackTrace();
                }

                while((str = bufferedReader.readLine()) != null)
                {
                    lineParser = new SplitLineParser(str);
                    lineParser.splitLine("\t");

                    try {
                        fetchResult = asyncFetch.run(((SplitLineParser) lineParser).getUrl(), ((SplitLineParser) lineParser).getKey(), 0);
                        targetFileReaderAndWriterMap.writeSuccess(fetchResult);
                    } catch (QiniuException e) {
                        // 抓取失败的异常
                        targetFileReaderAndWriterMap.writeErrorOrNull(e.error() + "\t" + lineParser.toString());
                    }
                }

                try {
                    bufferedReader.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            System.out.println("fetch completed for: " + targetFileDir);
        } catch (IOException ioException) {
            System.out.println("init stream writer or reader failed: " + ioException.getMessage() + ". it need retry.");
            ioException.printStackTrace();
        } finally {
            targetFileReaderAndWriterMap.closeReader();
            targetFileReaderAndWriterMap.closeWriter();
        }
    }
}