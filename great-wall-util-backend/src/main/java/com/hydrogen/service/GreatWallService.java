package com.hydrogen.service;

import com.hydrogen.model.Config;
import com.hydrogen.model.DeployMessage;
import com.hydrogen.model.DestroyInstanceRequest;
import com.hydrogen.model.InitDeployRequest;

import java.util.List;
import java.util.Map;

public interface GreatWallService {

    void saveConfig(Config saveConfigRequest);

    Config getConfig();

    Map<String, Map<String, Object>> listInstance();

    List<DeployMessage> getDeployMessage();

    boolean initDeploy(InitDeployRequest releaseAfterHour);

    void destroyInstance(DestroyInstanceRequest request);

}
