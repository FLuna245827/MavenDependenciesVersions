package org.flunadela.depsvers;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.RepositoryBase;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsHandler.class);

    private SettingsHandler() {
        // private constructor to prevent instantiation
    }

    public static List<String> getReleaseRepositoriesUrls() {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();

        SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserSettingsFile(new File(System.getProperty("user.home"), ".m2/settings.xml"));
        request.setGlobalSettingsFile(new File(System.getenv("M2_HOME"), "conf/settings.xml"));

        SettingsBuildingResult result;

        try {
            result = builder.build(request);
        } catch (SettingsBuildingException e) {
            LOGGER.warn("Error building Maven settings from settings.xml", e);
            return Collections.emptyList();
        }

        Settings allSettings = result.getEffectiveSettings();
        List<String> activeProfiles = allSettings.getActiveProfiles();

        List<Profile> declaredActiveProfiles;

        if (!activeProfiles.isEmpty()) {
            declaredActiveProfiles = allSettings.getProfiles().stream()
                    .filter(profile -> activeProfiles.contains(profile.getId())).toList();
        } else {
            declaredActiveProfiles = allSettings.getProfiles().stream()
                    .filter(profile -> profile.getActivation().isActiveByDefault()).toList();
        }

        return declaredActiveProfiles.stream()
                .flatMap(profile -> profile.getRepositories().stream())
                .filter(repo -> repo.getReleases().isEnabled())
                .map(RepositoryBase::getUrl)
                .filter(url -> !url.toLowerCase().startsWith(MavenMetadata.MAIN_REPO_URL))
                .toList();
    }
}
