package com.hydrogen.model;

public class DeployMessage {

    private DeployStep deployStep;
    private String msg;

    public DeployMessage(DeployStep deployStep, String msg) {
        this.deployStep = deployStep;
        this.msg = msg;
    }

    public DeployStep getDeployStep() {
        return deployStep;
    }

    public void setDeployStep(DeployStep deployStep) {
        this.deployStep = deployStep;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
