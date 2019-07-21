package com.hydrogen.model;

public enum DeployStep {
    PREPARE,
    CREATE_INSTANCE,
    DESTROY_INSTANCE,
    DEPLOY_BROOK_SERVER,
    DEPLOY_BROOK_CLIENT,
    DONE,
    FAIL,
}
