package com.hydrogen.utils;

import com.aliyuncs.IAcsClient;
import com.hydrogen.model.Config;

import java.io.Closeable;
import java.io.IOException;

public class ClientRef implements Closeable {

    private final IAcsClient client;

    private final Config config;

    public ClientRef(IAcsClient client, Config config) {
        this.client = client;
        this.config = config;
    }

    public IAcsClient getClient() {
        return client;
    }

    public Config getConfig() {
        return config;
    }

    @Override
    public void close() throws IOException {
        client.shutdown();
    }
}
