/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.autoconfigure.local;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.cloud.dataflow.server.config.CloudProfileProvider;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;


public class ProfileApplicationListener
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
    public static final String IGNORE_PROFILEAPPLICATIONLISTENER_PROPERTY_NAME = "spring.cloud.dataflow.server.profileapplicationlistener.ignore";
    public static final String IGNORE_PROFILEAPPLICATIONLISTENER_ENVVAR_NAME = "SPRING_CLOUD_DATAFLOW_SERVER_PROFILEAPPLICATIONLISTENER_IGNORE";
    private static final Logger logger = LoggerFactory.getLogger(ProfileApplicationListener.class);

    private ConfigurableEnvironment environment;

    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        this.environment = event.getEnvironment();
        Iterable<CloudProfileProvider> cloudProfileProviders = ServiceLoader.load(CloudProfileProvider.class);

        if (ignoreFromSystemProperty() ||
                ignoreFromEnvironmentVariable() ||
                cloudProfilesAlreadySet(cloudProfileProviders)) {
            return;
        }

        boolean addedCloudProfile = false;
        boolean addedKubernetesProfile = false;
        for (CloudProfileProvider cloudProfileProvider : cloudProfileProviders) {
            if (cloudProfileProvider.isCloudPlatform(this.environment)) {
                String profileToAdd = cloudProfileProvider.getCloudProfile();
                if (!Arrays.asList(this.environment.getActiveProfiles()).contains(profileToAdd)) {
                    if (profileToAdd.equals("kubernetes")) {
                        addedKubernetesProfile = true;
                    }
                    this.environment.addActiveProfile(profileToAdd);
                    addedCloudProfile = true;
                }
            }
        }

        if (!addedKubernetesProfile) {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("spring.cloud.kubernetes.enabled", Boolean.valueOf(false));
            logger.info("Setting property 'spring.cloud.kubernetes.enabled' to false.");
            MutablePropertySources propertySources = this.environment.getPropertySources();
            if (propertySources != null) {
                if (propertySources.contains("commandLineArgs")) {

                    propertySources.addAfter("commandLineArgs", new MapPropertySource("skipperProfileApplicationListener", properties));

                } else {

                    propertySources
                            .addFirst(new MapPropertySource("skipperProfileApplicationListener", properties));
                }
            }
        }

        if (!addedCloudProfile) {
            this.environment.addActiveProfile("local");
        }
    }


    private boolean ignoreFromSystemProperty() {
        return Boolean.getBoolean("spring.cloud.dataflow.server.profileapplicationlistener.ignore");
    }

    private boolean ignoreFromEnvironmentVariable() {
        return Boolean.parseBoolean(System.getenv("SPRING_CLOUD_DATAFLOW_SERVER_PROFILEAPPLICATIONLISTENER_IGNORE"));
    }


    public int getOrder() {
        return 0;
    }

    private boolean cloudProfilesAlreadySet(Iterable<CloudProfileProvider> cloudProfileProviders) {
        List<String> cloudProfileNames = new ArrayList<>();
        for (CloudProfileProvider cloudProfileProvider : cloudProfileProviders) {
            cloudProfileNames.add(cloudProfileProvider.getCloudProfile());
        }

        boolean cloudProfilesAlreadySet = false;
        for (String cloudProfileName : cloudProfileNames) {
            if (Arrays.asList(this.environment.getActiveProfiles()).contains(cloudProfileName)) {
                cloudProfilesAlreadySet = true;
            }
        }

        return cloudProfilesAlreadySet;
    }
}

