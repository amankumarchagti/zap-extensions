/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2021 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.addon.oast.services.boast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.extension.OptionsChangedListener;
import org.parosproxy.paros.model.OptionsParam;
import org.zaproxy.addon.oast.OastService;

public class BoastService extends OastService implements OptionsChangedListener {

    private static final Logger LOGGER = LogManager.getLogger(BoastService.class);

    private List<BoastServer> registeredServers = new ArrayList<>();
    private final ScheduledExecutorService executorService =
            Executors.newSingleThreadScheduledExecutor();

    private BoastParam boastParam;
    private int currentPollingFrequency;
    private ScheduledFuture<?> pollingSchedule;

    @Override
    public String getName() {
        return "BOAST";
    }

    @Override
    public void startService() {
        LOGGER.debug("Starting BOAST Service.");
        if (pollingSchedule == null || pollingSchedule.isDone()) {
            pollingSchedule =
                    executorService.scheduleAtFixedRate(
                            new BoastPoller(this),
                            getParam().getPollingFrequency(),
                            getParam().getPollingFrequency(),
                            TimeUnit.SECONDS);
        }
    }

    @Override
    public void stopService() {
        executorService.shutdown();
    }

    @Override
    public void sessionChanged() {
        registeredServers = new ArrayList<>();
    }

    public void optionsLoaded() {
        currentPollingFrequency = getParam().getPollingFrequency();
    }

    @Override
    public void optionsChanged(OptionsParam optionsParam) {
        if (currentPollingFrequency != getParam().getPollingFrequency()) {
            pollingSchedule.cancel(false);
            startService();
            currentPollingFrequency = getParam().getPollingFrequency();
            LOGGER.debug("Updated BOAST Polling frequency to {} seconds.", currentPollingFrequency);
        }
    }

    public BoastParam getParam() {
        if (boastParam == null) {
            boastParam = new BoastParam();
        }
        return boastParam;
    }

    public List<BoastServer> getRegisteredServers() {
        return registeredServers;
    }

    public BoastServer register(String boastUri) throws IOException {
        LOGGER.debug("Registering BOAST Server.");
        BoastServer boastServer = new BoastServer(boastUri);
        registeredServers.add(boastServer);
        return boastServer;
    }
}
