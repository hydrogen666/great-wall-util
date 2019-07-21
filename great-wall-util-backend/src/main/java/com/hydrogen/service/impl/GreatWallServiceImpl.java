package com.hydrogen.service.impl;

import com.aliyuncs.AcsRequest;
import com.aliyuncs.AcsResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.hydrogen.Entry;
import com.hydrogen.model.*;
import com.hydrogen.service.GreatWallService;
import com.hydrogen.utils.ClientRef;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

@Service
public class GreatWallServiceImpl implements GreatWallService {

    private final File configFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BlockingQueue<DeployMessage> deployMessages = new LinkedBlockingQueue<>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    private final AtomicBoolean deployInProgress = new AtomicBoolean(false);

    public GreatWallServiceImpl() throws IOException {
        String userHome = System.getProperty("user.home");
        Path path = Paths.get(userHome).resolve(".great-wall").resolve("config");
        this.configFile = new File(path.toString());
        configFile.getParentFile().mkdirs();
        configFile.createNewFile();
    }

    private ClientRef getClient() {
        Config config = getConfig();
        DefaultProfile defaultProfile = DefaultProfile.getProfile(
                config.getRegion(), config.getAccessKeyId(), config.getAccessKeySecret());
        IAcsClient client = new DefaultAcsClient(defaultProfile);
        return new ClientRef(client, config);
    }

    @Override
    public void saveConfig(Config saveConfigRequest) {
        JsonNode config = objectMapper.valueToTree(saveConfigRequest);
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            objectMapper.writeValue(out, config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Config getConfig() {
        try (FileInputStream in = new FileInputStream(configFile)) {
            return objectMapper.readValue(in, Config.class);
        } catch (IOException e) {
            return new Config();
        }
    }

    private <T extends AcsResponse> T getAcsResponse(Function<Config, AcsRequest<T>> requestFactory) {
        try (ClientRef clientRef = getClient()) {
            return clientRef.getClient().getAcsResponse(requestFactory.apply(clientRef.getConfig()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Map<String, Object>> listInstance() {
        DescribeInstancesResponse response = getAcsResponse((config -> {
            DescribeInstancesRequest request = new DescribeInstancesRequest();
            request.setSysRegionId(config.getRegion());
            return request;
        }));
        Map<String, Map<String, Object>> ret = new LinkedHashMap<>();
        for (DescribeInstancesResponse.Instance instance : response.getInstances()) {
            ret.put(instance.getInstanceId(), ImmutableMap.<String, Object>builder()
                    .put("instanceId", instance.getInstanceId())
                    .put("publicIp", instance.getPublicIpAddress())
                    .put("autoReleaseTime", instance.getAutoReleaseTime())
                    .build());
        }
        return ret;
    }

    private void addMessage(DeployStep step, String message) {
        readLock.lock();
        try {
            deployMessages.add(new DeployMessage(step, message));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean initDeploy(InitDeployRequest request) {
        if (!deployInProgress.compareAndSet(false, true)) {
            return false;
        }
        long timeoutMinutes = 10L;
        clearMessage();
        new Thread(() -> {
            try (ClientRef clientRef = getClient()) {
                Config config = clientRef.getConfig();
                new Entry(clientRef.getClient(),
                        Entry.ACTION_INIT_DEPLOY,
                        config.getRegion(),
                        request.getReleaseAfterHour(),
                        config.getRootPassword(),
                        timeoutMinutes,
                        null,
                        config.getBrookPassword(),
                        config.getBrookPort(),
                        this::addMessage).entry();
            } catch (Exception e) {
                addMessage(DeployStep.FAIL, e.getMessage());
            } finally {
                addMessage(DeployStep.DONE, "ALL DONE");
                deployInProgress.set(false);
            }
        }).start();
        return true;
    }

    @Override
    public void destroyInstance(DestroyInstanceRequest request) {
        try (ClientRef clientRef = getClient()) {
            new Entry(
                    clientRef.getClient(),
                    Entry.DESTROY_INSTANCE,
                    clientRef.getConfig().getRegion(),
                    1,
                    "",
                    0,
                    request.getInstanceId(),
                    "",
                    0,
                    (a, b) -> {}
            ).entry();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void clearMessage() {
        writeLock.lock();
        try {
            deployMessages.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<DeployMessage> getDeployMessage() {
        writeLock.lock();
        try {
            return new ArrayList<>(deployMessages);
        } finally {
            writeLock.unlock();
        }
    }
}