package com.hydrogen;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.AuthorizeSecurityGroupRequest;
import com.aliyuncs.ecs.model.v20140526.CreateCommandRequest;
import com.aliyuncs.ecs.model.v20140526.CreateCommandResponse;
import com.aliyuncs.ecs.model.v20140526.CreateSecurityGroupRequest;
import com.aliyuncs.ecs.model.v20140526.CreateSecurityGroupResponse;
import com.aliyuncs.ecs.model.v20140526.CreateVSwitchRequest;
import com.aliyuncs.ecs.model.v20140526.CreateVSwitchResponse;
import com.aliyuncs.ecs.model.v20140526.CreateVpcRequest;
import com.aliyuncs.ecs.model.v20140526.CreateVpcResponse;
import com.aliyuncs.ecs.model.v20140526.DeleteInstanceRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeCommandsRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeCommandsResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeInstancesResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupsRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeSecurityGroupsResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeVSwitchesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeVSwitchesResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeVpcsRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeVpcsResponse;
import com.aliyuncs.ecs.model.v20140526.DescribeZonesRequest;
import com.aliyuncs.ecs.model.v20140526.DescribeZonesResponse;
import com.aliyuncs.ecs.model.v20140526.InvokeCommandRequest;
import com.aliyuncs.ecs.model.v20140526.RunInstancesRequest;
import com.aliyuncs.ecs.model.v20140526.RunInstancesResponse;
import com.aliyuncs.exceptions.ClientException;
import com.hydrogen.model.DeployStep;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

/**
 * @author hydrogen
 */
public class Entry {

    /**
     * 是否只预检此次请求。true：发送检查请求，不会创建实例，也不会产生费用；false：发送正常请求，通过检查后直接创建实例，并直接产生费用
     */
    private final boolean dryRun = false;
    /**
     * 实例的资源规格
     */
    private final String instanceType = "ecs.xn4.small";
    /**
     * 实例的计费方式
     */
    private final String instanceChargeType = "PostPaid";
    /**
     * 镜像ID
     */
    private final String imageId = "ubuntu_18_04_64_20G_alibase_20190509.vhd";
    /**
     * 购买资源的时长
     */
    private final Integer period = 1;
    /**
     * 购买资源的时长单位
     */
    private final String periodUnit = "Hourly";
    /**
     * 网络计费类型
     */
    private final String internetChargeType = "PayByTraffic";
    /**
     * 指定创建ECS实例的数量
     */
    private final Integer amount = 1;
    /**
     * 公网出带宽最大值
     */
    private final Integer internetMaxBandwidthOut = 5;
    /**
     * 是否为I/O优化实例
     */
    private final String ioOptimized = "optimized";
    /**
     * 后付费实例的抢占策略
     */
    private final String spotStrategy = "SpotAsPriceGo";
    /**
     * 是否开启安全加固
     */
    private final String securityEnhancementStrategy = "Active";
    /**
     * 自动释放时间
     */
    private final String autoReleaseTime;
    /**
     * 系统盘大小
     */
    private final String systemDiskSize = "20";
    /**
     * 系统盘的磁盘种类
     */
    private final String systemDiskCategory = "cloud_efficiency";

    private final String deployBrookCommandName = "deploy_brook";

    private final String rootPassword;

    private final long waitRunningTimeoutMinutes;

    private final String brookPassword;

    private final String brookPort;

    // params for destroy_instance
    private final String instanceId;

    public static final String ACTION_INIT_DEPLOY = "init_deploy";
    public static final String DEPLOY_BROOK = "deploy_brook";
    public static final String DEPLOY_BROOK_CLIENT = "deploy_brook_client";
    public static final String DESTROY_INSTANCE = "destroy_instance";

    private final IAcsClient client;

    private final String action;

    private final String region;

    private final DescribeZonesResponse.Zone zone;

    private final BiConsumer<DeployStep, String> deployMessageConsumer;

    private final DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC().withLocale(Locale.US);

    public Entry(IAcsClient client,
                 String action,
                 String region,
                 int releaseAfterHour,
                 String rootPassword,
                 long waitRunningTimeoutMinutes,
                 String instanceId,
                 String brookPassword,
                 Integer brookPort,
                 BiConsumer<DeployStep, String> deployMessageConsumer) throws ClientException {
        this.client = client;
        this.action = action;
        this.region = region;
        autoReleaseTime = dateTimeFormat.print(new DateTime().plusHours(releaseAfterHour));
        this.rootPassword = rootPassword;
        this.waitRunningTimeoutMinutes = waitRunningTimeoutMinutes;
        this.instanceId = instanceId;
        this.brookPassword = brookPassword;
        this.brookPort = brookPort.toString();
        this.deployMessageConsumer = deployMessageConsumer;
        if (Objects.equals(action, ACTION_INIT_DEPLOY)) {
            this.zone = filterAvailableZone();
        } else {
            this.zone = null;
        }
    }

    public void entry() throws Exception {
        switch (action) {
            case ACTION_INIT_DEPLOY:
                initDeploy();
                break;
            case DEPLOY_BROOK:
                deployBrook(instanceId);
                break;
            case DEPLOY_BROOK_CLIENT:
                deployBrookClient(instanceId);
                break;
            case DESTROY_INSTANCE:
                destroyInstance();
                break;
            default:
                break;
        }
    }

    private RunInstancesRequest runInstancesRequest() throws ClientException {
        DescribeVpcsResponse.Vpc vpc = describeVpc();
        DescribeSecurityGroupsResponse.SecurityGroup securityGroup = describeSecurityGroup(vpc);
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
        runInstancesRequest.setDryRun(dryRun);
        runInstancesRequest.setSysRegionId(region);
        runInstancesRequest.setInstanceType(instanceType);
        runInstancesRequest.setInstanceChargeType(instanceChargeType);
        runInstancesRequest.setImageId(imageId);
        runInstancesRequest.setSecurityGroupId(securityGroup.getSecurityGroupId());
        runInstancesRequest.setPeriod(period);
        runInstancesRequest.setPeriodUnit(periodUnit);
        runInstancesRequest.setZoneId(zone.getZoneId());
        runInstancesRequest.setInternetChargeType(internetChargeType);
        runInstancesRequest.setVSwitchId(describeVSwitch(vpc));
        runInstancesRequest.setInstanceName("my-ecs-" + dateTimeFormat.print(System.currentTimeMillis()));
        runInstancesRequest.setAmount(amount);
        runInstancesRequest.setInternetMaxBandwidthOut(internetMaxBandwidthOut);
        runInstancesRequest.setIoOptimized(ioOptimized);
        runInstancesRequest.setSpotStrategy(spotStrategy);
        runInstancesRequest.setSecurityEnhancementStrategy(securityEnhancementStrategy);
        runInstancesRequest.setAutoReleaseTime(autoReleaseTime);
        runInstancesRequest.setSystemDiskSize(systemDiskSize);
        runInstancesRequest.setSystemDiskCategory(systemDiskCategory);
        runInstancesRequest.setPassword(rootPassword);
        return runInstancesRequest;
    }

    private DescribeSecurityGroupsResponse.SecurityGroup createSecurityGroup(String vpcId) throws ClientException {
        deployMessageConsumer.accept(DeployStep.PREPARE, "Creating security group...");
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
        request.setSysRegionId(region);
        request.setSecurityGroupType("normal");
        request.setVpcId(vpcId);
        CreateSecurityGroupResponse response = client.getAcsResponse(request);

        DescribeSecurityGroupsRequest describe = new DescribeSecurityGroupsRequest();
        describe.setSecurityGroupId(response.getSecurityGroupId());
        describe.setSysRegionId(region);
        return client.getAcsResponse(describe, region).getSecurityGroups().get(0);
    }

    private void authorizeSecurityGroup(String securityGroupId, String protocol, String portRange) throws ClientException {
        AuthorizeSecurityGroupRequest request = new AuthorizeSecurityGroupRequest();
        request.setSysRegionId(region);
        request.setIpProtocol(protocol);
        request.setPortRange(portRange);
        request.setSecurityGroupId(securityGroupId);
        request.setSourceCidrIp("0.0.0.0/0");
        client.getAcsResponse(request);
    }

    private String brookPortRange() {
        return brookPort + "/" + brookPort;
    }

    private DescribeSecurityGroupsResponse.SecurityGroup describeSecurityGroup(DescribeVpcsResponse.Vpc vpc) throws ClientException {
        final DescribeSecurityGroupsResponse.SecurityGroup securityGroup;
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        request.setVpcId(vpc.getVpcId());
        DescribeSecurityGroupsResponse response = client.getAcsResponse(request);
        List<DescribeSecurityGroupsResponse.SecurityGroup> groupList = response.getSecurityGroups();
        if (null != groupList && !groupList.isEmpty()) {
            securityGroup = groupList.get(0);
        } else {
            securityGroup = createSecurityGroup(vpc.getVpcId());
        }
        String securityGroupId = securityGroup.getSecurityGroupId();
        authorizeSecurityGroup(securityGroupId, "UDP", brookPortRange());
        authorizeSecurityGroup(securityGroupId, "TCP", brookPortRange());
        authorizeSecurityGroup(securityGroupId, "ICMP", "-1/-1");
        authorizeSecurityGroup(securityGroupId, "TCP", "22/22");
        return securityGroup;
    }

    private DescribeVpcsResponse.Vpc createVpc() throws ClientException {
        CreateVpcRequest request = new CreateVpcRequest();
        request.setSysRegionId(region);
        request.setCidrBlock("172.16.0.0/12");
        CreateVpcResponse response = client.getAcsResponse(request);
        DescribeVpcsRequest describeVpcsRequest = new DescribeVpcsRequest();
        describeVpcsRequest.setSysRegionId(region);
        describeVpcsRequest.setVpcId(response.getVpcId());
        return waitResourceReady(() -> client.getAcsResponse(describeVpcsRequest),
                DescribeVpcsResponse::getVpcs,
                (vpc) -> Objects.equals(vpc.getStatus(), "Available"),
                () -> deployMessageConsumer.accept(DeployStep.PREPARE, "VPC [" + response.getVpcId() + "] not ready yet..."),
                (res) -> deployMessageConsumer.accept(DeployStep.PREPARE, "VPC [" + response.getVpcId() + "] ready!"));
    }

    private DescribeVpcsResponse.Vpc describeVpc() throws ClientException {
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        request.setSysRegionId(region);
        DescribeVpcsResponse response = client.getAcsResponse(request, region);
        if (null == response.getVpcs() || response.getVpcs().isEmpty()) {
            return createVpc();
        } else {
            return response.getVpcs().get(0);
        }
    }

    private DescribeZonesResponse.Zone filterAvailableZone() throws ClientException {
        DescribeZonesRequest request = new DescribeZonesRequest();
        request.setSysRegionId(region);
        DescribeZonesResponse response = client.getAcsResponse(request);
        if (null == response.getZones() || response.getZones().isEmpty()) {
            throw new IllegalStateException("Zones is empty");
        }
        for (DescribeZonesResponse.Zone zone : response.getZones()) {
            zone.getAvailableDiskCategories();
            for (DescribeZonesResponse.Zone.ResourcesInfo resourcesInfo : zone.getAvailableResources()) {
                if (resourcesInfo.getSystemDiskCategories().contains(this.systemDiskCategory)
                        && resourcesInfo.getInstanceTypes().contains(this.instanceType)) {
                    return zone;
                }
            }
        }
        throw new IllegalStateException("Available zone not found");
    }

    private <RES, RESP> RES waitResourceReady(Describer<RESP> describer,
                                              Function<RESP, List<RES>> resourceGetter,
                                              Predicate<RES> checker,
                                              Runnable notReadyLogger,
                                              Consumer<RES> readyLogger) throws ClientException {
        long timeoutNano = TimeUnit.NANOSECONDS.convert(waitRunningTimeoutMinutes, TimeUnit.MINUTES);
        while (timeoutNano >= 0) {
            long start = System.nanoTime();
            RESP resp = describer.describe();
            List<RES> resource = resourceGetter.apply(resp);
            if (null != resource && !resource.isEmpty() && checker.test(resource.get(0))) {
                readyLogger.accept(resource.get(0));
                return resource.get(0);
            }
            notReadyLogger.run();
            try {
                Thread.sleep(1_000L);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Wait ready Interrupted", e);
            }
            timeoutNano -= System.nanoTime() - start;
        }
        throw new IllegalStateException("Timeout!");
    }

    @FunctionalInterface
    interface Describer<RESP> {
        RESP describe() throws ClientException;
    }

    private void waitVSwitchReady(String vSwitchId) throws ClientException {
        DescribeVSwitchesRequest request = new DescribeVSwitchesRequest();
        request.setSysRegionId(region);
        request.setZoneId(zone.getZoneId());
        request.setVSwitchId(vSwitchId);

        waitResourceReady(() -> client.getAcsResponse(request),
                DescribeVSwitchesResponse::getVSwitches,
                (vSwitch -> Objects.equals(vSwitch.getStatus(), "Available")),
                () ->  deployMessageConsumer.accept(DeployStep.PREPARE ,"VSwitch [" + vSwitchId + "] not ready yet..."),
                (res) -> deployMessageConsumer.accept(DeployStep.PREPARE, "VSwitch [" + vSwitchId + "] ready!"));

    }

    private String createVSWitch(DescribeVpcsResponse.Vpc vpc) throws ClientException {
        deployMessageConsumer.accept(DeployStep.PREPARE, "Creating VSwitch for [" +  vpc.getVpcId() + "]");
        CreateVSwitchRequest request = new CreateVSwitchRequest();
        request.setCidrBlock("172.16.0.0/16");
        request.setZoneId(zone.getZoneId());
        request.setSysRegionId(region);
        request.setVpcId(vpc.getVpcId());

        CreateVSwitchResponse response = client.getAcsResponse(request, region);
        waitVSwitchReady(response.getVSwitchId());
        return response.getVSwitchId();
    }

    private String describeVSwitch(DescribeVpcsResponse.Vpc vpc) throws ClientException {
        if (null == vpc) {
            return null;
        }
        DescribeVSwitchesRequest request = new DescribeVSwitchesRequest();
        request.setSysRegionId(region);
        request.setZoneId(zone.getZoneId());
        DescribeVSwitchesResponse response = client.getAcsResponse(request, region);
        if (response.getVSwitches() == null || response.getVSwitches().isEmpty()) {
            return createVSWitch(vpc);
        } else {
            return response.getVSwitches().get(0).getVSwitchId();
        }
    }

    private DescribeInstancesResponse describeInstance(String instanceId) throws ClientException {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setInstanceIds("[\"" + instanceId + "\"]");
        request.setSysRegionId(region);
        return client.getAcsResponse(request);
    }

    private DescribeInstancesResponse.Instance waitInstanceRunning(String instanceId) throws Exception {
        return waitResourceReady(() -> describeInstance(instanceId), DescribeInstancesResponse::getInstances,
                instance -> "Running".equals(instance.getStatus()),
                () -> deployMessageConsumer.accept(DeployStep.CREATE_INSTANCE, "Instance [" + instanceId + "] not ready yet..."),
                (res) -> deployMessageConsumer.accept(DeployStep.CREATE_INSTANCE,
                        String.format("Instance [%s] Ready! public ip:[%s]", instanceId, res.getPublicIpAddress())));
    }

    private String runInstance() throws Exception {
        RunInstancesRequest request = runInstancesRequest();
        deployMessageConsumer.accept(DeployStep.CREATE_INSTANCE, "Creating instance...");
        RunInstancesResponse response = client.getAcsResponse(request, region);
        deployMessageConsumer.accept(DeployStep.CREATE_INSTANCE, "Creating instance done! instanceId:[" + response.getInstanceIdSets().get(0) + "]");
        DescribeInstancesResponse.Instance instance = waitInstanceRunning(response.getInstanceIdSets().get(0));
        return instance.getInstanceId();
    }

    private void initDeploy() throws Exception {
        // create ecs instance
        String instanceId = runInstance();
        deployBrook(instanceId);
    }

    private void destroyInstance() throws Exception {
        deployMessageConsumer.accept(DeployStep.DESTROY_INSTANCE, "Destroying instance [" + instanceId + "]...");
        DeleteInstanceRequest request = new DeleteInstanceRequest();
        request.setSysRegionId(region);
        request.setInstanceId(instanceId);
        request.setForce(true);
        client.getAcsResponse(request);
    }

    private String getDeployBrookScript() throws Exception {
        DescribeCommandsRequest describeCommandsRequest = new DescribeCommandsRequest();
        describeCommandsRequest.setName(deployBrookCommandName);
        describeCommandsRequest.setSysRegionId(region);

        DescribeCommandsResponse describeCommandsResponse = client.getAcsResponse(describeCommandsRequest, region);
        for (DescribeCommandsResponse.Command command : describeCommandsResponse.getCommands()) {
            if (Objects.equals(command.getName(), deployBrookCommandName)) {
                return command.getCommandId();
            }
        }
        // command not found
        CreateCommandRequest createCommandRequest = new CreateCommandRequest();
        createCommandRequest.setSysRegionId(region);

        createCommandRequest.setName(deployBrookCommandName);
        createCommandRequest.setType("RunShellScript");

        try (InputStream scriptFile = getClass().getClassLoader().getResourceAsStream("deploy_brook.sh.tpl")) {
            createCommandRequest.setCommandContent(Base64.encodeBase64String(IOUtils.toByteArray(scriptFile)));
            createCommandRequest.setEnableParameter(true);
            createCommandRequest.setWorkingDir("/root");

            CreateCommandResponse createCommandResponse = client.getAcsResponse(createCommandRequest);
            return createCommandResponse.getCommandId();
        }
    }

    private InputStream getResourceInputStream(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    private byte[] readResourceContent(String resource) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            return IOUtils.toByteArray(is);
        }
    }

    private void copyContent(Supplier<InputStream> srcProvider, String target) throws Exception {
        File targetFile = new File(target);
        String srcMd5;
        try (InputStream is = srcProvider.get()) {
            srcMd5 = DigestUtils.md5Hex(is);
        }
        if (targetFile.exists() && targetFile.isDirectory()) {
            targetFile.delete();
        }
        final boolean moveFile;
        if (targetFile.exists() && targetFile.isFile()) {
            // is file check checksum
            try (InputStream is = new FileInputStream(targetFile)) {
                String originMd5 = DigestUtils.md5Hex(is);
                moveFile = !Objects.equals(originMd5, srcMd5);
            }
        } else {
            moveFile = true;
        }
        if (moveFile) {
            try (FileOutputStream out = new FileOutputStream(targetFile);
                 InputStream is = srcProvider.get()) {
                IOUtils.copy(is, out);
            }
        }
    }

    private void deployBrookClient(String instanceId) throws Exception {
        final String brookClientTarget = "/usr/local/bin/brook_darwin_amd64";
        final String startBrookClientTarget = "/usr/local/bin/start_brook.sh";
        deployMessageConsumer.accept(DeployStep.DEPLOY_BROOK_CLIENT, "Deploying brook client...");
        // describe instance
        DescribeInstancesResponse describeInstancesResponse = describeInstance(instanceId);
        String serverIp;
        try {
            serverIp = describeInstancesResponse.getInstances().get(0).getPublicIpAddress().get(0);
        } catch (Exception e) {
            throw new IllegalStateException("Describe instance failed", e);
        }

        String brookServicePath = Paths.get(System.getProperty("user.home"))
                .resolve("Library")
                .resolve("LaunchAgents")
                .resolve("com.brook.plist").toString();
        copyContent(() -> getResourceInputStream("brook_darwin_amd64"), brookClientTarget);
        copyContent(() -> getResourceInputStream("com.brook.plist"), brookServicePath);

        final String startBrookScript = MessageFormat.format(new String(readResourceContent("start_brook.sh"), StandardCharsets.UTF_8),
                serverIp, brookPort);

        copyContent(() -> new ByteArrayInputStream(startBrookScript.getBytes(StandardCharsets.UTF_8)),
                startBrookClientTarget);

        Runtime.getRuntime().exec(String.format("chmod +x %s", brookClientTarget));
        Runtime.getRuntime().exec(String.format("chmod +x %s", startBrookClientTarget));

        deployMessageConsumer.accept(DeployStep.DEPLOY_BROOK_CLIENT, "Restart brook client...");
        Runtime.getRuntime().exec(String.format("launchctl unload %s", brookServicePath));
        Runtime.getRuntime().exec(String.format("launchctl load %s", brookServicePath));
        Runtime.getRuntime().exec(String.format("launchctl stop %s", brookServicePath));
        Runtime.getRuntime().exec(String.format("launchctl start %s", brookServicePath));
    }

    private void deployBrook(String instanceId) throws Exception {
        deployMessageConsumer.accept(DeployStep.DEPLOY_BROOK_SERVER, "Deploying brook server...");
        // deploy brook server
        String commandId = getDeployBrookScript();
        InvokeCommandRequest invokeCommandRequest = new InvokeCommandRequest();
        invokeCommandRequest.setSysRegionId(region);
        invokeCommandRequest.setCommandId(commandId);
        Map<Object, Object> parameters = new HashMap<>();
        parameters.put("brook_server_port", brookPort);
        parameters.put("brook_password", brookPassword);
        invokeCommandRequest.setParameters(parameters);
        invokeCommandRequest.setInstanceIds(Collections.singletonList(instanceId));
        client.getAcsResponse(invokeCommandRequest, region);
        // deploy client
        deployBrookClient(instanceId);
    }
}
