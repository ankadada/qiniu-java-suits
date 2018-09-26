package com.qiniu.model;

public class FileTypeParams extends BaseParams {

    private String bucket;
    private String targetType;
    private PointTimeParams pointTimeParams;

    public FileTypeParams(String[] args) throws Exception {
        super(args);
        pointTimeParams = new PointTimeParams(args);
        this.bucket = getParam("bucket");
        this.targetType = getParam("type");
        super.setSelfName("type");
    }

    public String getBucket() {
        return bucket;
    }

    public short getTargetType() throws Exception {
        if (targetType.matches("(0|1)")) {
            return Short.valueOf(targetType);
        } else {
            throw new Exception("the direction is incorrect, please set it 0 or 1");
        }
    }

    public PointTimeParams getPointTimeParams() {
        return pointTimeParams;
    }
}