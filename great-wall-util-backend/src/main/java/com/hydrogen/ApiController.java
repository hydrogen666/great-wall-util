package com.hydrogen;

import com.google.common.collect.ImmutableMap;
import com.hydrogen.model.Config;
import com.hydrogen.model.InstanceIdRequest;
import com.hydrogen.model.InitDeployRequest;
import com.hydrogen.service.GreatWallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private GreatWallService greatWallService;

    @GetMapping("/get_config")
    public Object getConfig() {
        return greatWallService.getConfig();
    }

    @PostMapping(value = "/save_config", consumes = "application/json")
    public Object saveConfig(@RequestBody Config request) {
        greatWallService.saveConfig(request);
        return request;
    }

    @GetMapping("/list_instance")
    public Object listInstance() {
        return greatWallService.listInstance();
    }

    @PostMapping("/init_deploy")
    public Object initDeploy(@RequestBody InitDeployRequest request) {
        boolean success = greatWallService.initDeploy(request);
        return ImmutableMap.builder().put("success", success).build();
    }

    @PostMapping("/destroy_instance")
    public Object destroyInstance(@RequestBody InstanceIdRequest request) {
        greatWallService.destroyInstance(request);
        return Collections.emptyMap();
    }

    @PostMapping("/deploy_brook_server")
    public Object deployBrookServer(@RequestBody InstanceIdRequest request) {
        greatWallService.deployBrookServer(request);
        return Collections.emptyMap();
    }

    @PostMapping("/deploy_brook_client")
    public Object deployBrookClient(@RequestBody InstanceIdRequest request) {
        greatWallService.deployBrookClient(request);
        return Collections.emptyMap();
    }

    @GetMapping("/get_deploy_message")
    public Object getDeployMessage() {
        return greatWallService.getDeployMessage();
    }
}
