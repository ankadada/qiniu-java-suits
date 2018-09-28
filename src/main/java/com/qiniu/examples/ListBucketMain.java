package com.qiniu.examples;

import com.qiniu.common.*;
import com.qiniu.interfaces.IBucketProcess;
import com.qiniu.interfaces.IOssFileProcess;
import com.qiniu.model.*;
import com.qiniu.service.impl.*;
import com.qiniu.storage.Configuration;

public class ListBucketMain {

    public static void main(String[] args) throws Exception {

        String configFile = ".qiniu.properties";
        boolean paramFromConfig = (args == null || args.length == 0);
        ListBucketParams listBucketParams = paramFromConfig ?
                new ListBucketParams(configFile) : new ListBucketParams(args);
        String accessKey = listBucketParams.getAccessKey();
        String secretKey = listBucketParams.getSecretKey();
        String bucket = listBucketParams.getBucket();
        String resultFileDir = listBucketParams.getResultFileDir();
        int maxThreads = listBucketParams.getMaxThreads();
        int version = listBucketParams.getVersion();
        boolean withParallel = listBucketParams.getWithParallel();
        int level = listBucketParams.getLevel();
        String process = listBucketParams.getProcess();
        int unitLen = listBucketParams.getUnitLen();
        unitLen = (version == 1 && unitLen > 1000) ? unitLen%1000 : unitLen;
        IOssFileProcess iOssFileProcessor = null;
        QiniuAuth auth = QiniuAuth.create(accessKey, secretKey);
        Configuration configuration = new Configuration(Zone.autoZone());

        switch (process) {
            // isBiggerThan 标志为 true 时，在 pointTime 时间点之前的记录进行处理，isBiggerThan 标志为 false 时，在 pointTime 时间点之后的记录进行处理。
            case "status":
                FileStatusParams fileStatusParams = paramFromConfig ? new FileStatusParams(configFile) : new FileStatusParams(args);
                PointTimeParams pointTimeParams = fileStatusParams.getPointTimeParams();
                iOssFileProcessor = new ChangeStatusProcess(auth, fileStatusParams.getBucket(), fileStatusParams.getTargetStatus(),
                        resultFileDir, pointTimeParams.getPointDate() + " " + pointTimeParams.getPointTime(),
                        pointTimeParams.getDirection());
                break;
            case "type":
                FileTypeParams fileTypeParams = paramFromConfig ? new FileTypeParams(configFile) : new FileTypeParams(args);
                pointTimeParams = fileTypeParams.getPointTimeParams();
                iOssFileProcessor = new ChangeTypeProcess(auth, configuration, fileTypeParams.getBucket(), fileTypeParams.getTargetType(),
                        resultFileDir, pointTimeParams.getPointDate() + " " + pointTimeParams.getPointTime(),
                        pointTimeParams.getDirection());
                break;
            case "copy":
                FileCopyParams fileCopyParams = paramFromConfig ? new FileCopyParams(configFile) : new FileCopyParams(args);
                accessKey = "".equals(fileCopyParams.getAKey()) ? accessKey : fileCopyParams.getAKey();
                secretKey = "".equals(fileCopyParams.getSKey()) ? secretKey : fileCopyParams.getSKey();
                iOssFileProcessor = new BucketCopyProcess(QiniuAuth.create(accessKey, secretKey), configuration, fileCopyParams.getSourceBucket(),
                        fileCopyParams.getTargetBucket(), fileCopyParams.getTargetKeyPrefix(), resultFileDir);
                break;
        }

        IBucketProcess listBucketMultiProcessor = new ListBucketProcess(auth, configuration, bucket, resultFileDir);
        try {
            listBucketMultiProcessor.processBucketWithEndFile(iOssFileProcessor, version, maxThreads, withParallel, level, unitLen);
        } catch (QiniuException e) {
            System.out.println(e.getMessage());
        } finally {
            if (iOssFileProcessor != null)
                iOssFileProcessor.closeResource();
            listBucketMultiProcessor.closeResource();
        }
    }
}