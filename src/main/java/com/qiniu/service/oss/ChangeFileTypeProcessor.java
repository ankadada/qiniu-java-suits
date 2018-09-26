package com.qiniu.service.oss;

import com.qiniu.common.QiniuAuth;
import com.qiniu.common.QiniuBucketManager;
import com.qiniu.common.QiniuBucketManager.*;
import com.qiniu.common.QiniuException;
import com.qiniu.common.QiniuSuitsException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.util.JSONConvertUtils;

public class ChangeFileTypeProcessor {

    private QiniuBucketManager bucketManager;

    private static volatile ChangeFileTypeProcessor changeFileTypeProcessor = null;

    public ChangeFileTypeProcessor(QiniuAuth auth, Configuration configuration) {
        this.bucketManager = new QiniuBucketManager(auth, configuration);
    }

    public static ChangeFileTypeProcessor getChangeFileTypeProcessor(QiniuAuth auth, Configuration configuration) {
        if (changeFileTypeProcessor == null) {
            synchronized (ChangeFileTypeProcessor.class) {
                if (changeFileTypeProcessor == null) {
                    changeFileTypeProcessor = new ChangeFileTypeProcessor(auth, configuration);
                }
            }
        }
        return changeFileTypeProcessor;
    }

    public String doFileTypeChange(String bucket, String key, StorageType type) throws QiniuSuitsException {
        Response response = null;
        String respBody = "";

        try {
            response = bucketManager.changeType(bucket, key, type);
            respBody = response.bodyString();
        } catch (QiniuException e) {
            QiniuSuitsException qiniuSuitsException = new QiniuSuitsException("change file type error");
            qiniuSuitsException.addToFieldMap("code", String.valueOf(e.code()));
            qiniuSuitsException.addToFieldMap("error", String.valueOf(e.error()));
            qiniuSuitsException.setStackTrace(e.getStackTrace());
            throw qiniuSuitsException;
        } finally {
            if (response != null)
                response.close();
        }

        return response.statusCode + "\t" + response.reqId + "\t" + respBody;
    }

    private Response changeTypeWithRetry(String bucket, String key, StorageType type, int retryCount) throws QiniuSuitsException, QiniuException {
        Response response;

        try {
            response = bucketManager.changeType(bucket, key, type);
        } catch (QiniuException e) {
            retryCount--;
            if (retryCount > 0 && String.valueOf(e.code()).matches("^[-015]\\d{0,2}")) {
                System.out.println(e.getMessage() + ", last " + retryCount + " times retry...");
                retryCount--;
                response = bucketManager.changeType(bucket, key, type);
            } else {
                throw new QiniuSuitsException(e);
            }
        }

        return response;
    }

    public void closeBucketManager() {
        if (bucketManager != null)
            bucketManager.closeResponse();
    }
}