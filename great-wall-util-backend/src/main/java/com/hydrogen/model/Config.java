package com.hydrogen.model;

public class Config {

    private String region;

    private String accessKeyId;

    private String accessKeySecret;

    private String rootPassword;

    private String brookPassword;

    private Integer brookPort;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    public String getBrookPassword() {
        return brookPassword;
    }

    public void setBrookPassword(String brookPassword) {
        this.brookPassword = brookPassword;
    }

    public Integer getBrookPort() {
        return brookPort;
    }

    public void setBrookPort(Integer brookPort) {
        this.brookPort = brookPort;
    }
}
