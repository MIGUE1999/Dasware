package com.bluetooth.perifericoble;

public class User {

    private String name;
    private String authorizationId;
    private String idType;
    private String appId;
    private String nonceABF;

    public User(){
        name = null;
        authorizationId = null;
        idType = null;
        appId = null;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdType() {
        return idType;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppId() {
        return appId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setNonceABF(String nonceABF) {
        this.nonceABF = nonceABF;
    }

    public String getNonceABF() {
        return nonceABF;
    }
}
