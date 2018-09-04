package com.qiniu.examples;

import com.qiniu.common.QiniuSuitsException;
import com.qiniu.common.FileReaderAndWriterMap;
import com.qiniu.common.QiniuAuth;
import com.qiniu.config.PropertyConfig;
import com.qiniu.interfaces.ILineParser;
import com.qiniu.service.impl.SplitLineParser;
import com.qiniu.service.oss.AsyncFetchProcessor;

import java.io.BufferedReader;
import java.io.IOException;

public class FetchMain {

    public static void main(String[] args) throws QiniuSuitsException {

        PropertyConfig propertyConfig = new PropertyConfig(".qiniu.properties");
        String ak = propertyConfig.getProperty("access_key");
        String sk = propertyConfig.getProperty("secret_key");
        String bucket = propertyConfig.getProperty("bucket");
        String targetFileDir = "/Users/wubingheng/Public/Works/myaccount";
        String sourceFileDir = "/Users/wubingheng/Public/Works/myaccount";
        String[] sourceReaders = new String[]{"xaa", "xab", "xac", "xad", "xae", "xaf", "xag", "xah", "xai", "xaj", "xak", "xal"};

        FileReaderAndWriterMap targetFileReaderAndWriterMap = new FileReaderAndWriterMap();
        AsyncFetchProcessor asyncFetchProcessor = AsyncFetchProcessor.getAsyncFetchProcessor(QiniuAuth.create(ak, sk), bucket);
        String str = null;

        try {
            targetFileReaderAndWriterMap.initOutputStreamWriter(targetFileDir, "fetch");
            targetFileReaderAndWriterMap.initInputStreamReader(sourceFileDir);
            System.out.println("fetch started...");
            BufferedReader bufferedReader = null;
            String fetchResult = "";
            ILineParser lineParser = null;

            for (int i = 0; i < sourceReaders.length; i++) {

                try {
                    bufferedReader = new BufferedReader(targetFileReaderAndWriterMap.getInputStreamReader(sourceReaders[i]));
                } catch (NullPointerException nullPointerException) {
                    nullPointerException.printStackTrace();
                }

                while((str = bufferedReader.readLine()) != null)
                {
                    lineParser = new SplitLineParser(str);

                    try {
                        fetchResult = asyncFetchProcessor.doAsyncFetch(lineParser.getUrl(), lineParser.getKey());
                        targetFileReaderAndWriterMap.writeSuccess(fetchResult);
                    } catch (QiniuSuitsException e) {
                        // 抓取失败的异常
                        targetFileReaderAndWriterMap.writeErrorAndNull(e.toString() + "\t" + lineParser.toString());
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
            targetFileReaderAndWriterMap.closeStreamReader();
            targetFileReaderAndWriterMap.closeStreamWriter();
        }

        asyncFetchProcessor.closeClient();
    }
}