package com.epa;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.RestartDialogImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class PluginStartupActivity implements StartupActivity {

    private static final Logger LOG = Logger.getInstance(PluginStartupActivity.class);
    public static final String PLUGIN_ID = "com.intellij.modules.go-capable";
    public static final String INTELLIJ_RELEASE_URL = "https://www.jetbrains.com/intellij-repository/releases/";
    public static final String EXECUTION_PROCESS_ELEVATION = "execution-process-elevation";
    public static final String JAR = ".jar";
    public static final String EXECUTION_PROCESS_MEDIATOR_CLIENT = "execution-process-mediator-client";
    public static final String INTELLIJ_PACKAGE = "com/jetbrains/intellij/";
    public static final String EXECUTION_PACKAGE = INTELLIJ_PACKAGE + "execution/";
    public static final String LIBRARIES_PACKAGE = INTELLIJ_PACKAGE + "libraries/";
    public static final String EXECUTION_EXECUTION_PROCESS_MEDIATOR_COMMON = "execution-process-mediator-common";
    public static final String EXECUTION_PROCESS_MEDIATOR_DAEMON = "execution-process-mediator-daemon";
    public static final String LIBRARIES_GRPC = "libraries-grpc";
    public static final String LIBRARIES_GRPC_NETTY_SHADED = "libraries-grpc-netty-shaded";

    @Override
    public void runActivity(@NotNull Project project) {
        PluginInstallState state = ApplicationManager.getApplication().getService(PluginInstallState.class);
        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        String buildNumber = appInfo.getBuild().asString().split("-")[1];
        String path = getPath();
        Map<String, String> jarUrls = getUrls(buildNumber);
        if (!buildNumber.equals(state.getVersion()) || !checkFilesExist(path, jarUrls)) {
            downloadJars(path, jarUrls);
            state.setVersion(buildNumber);
            int result = Messages.showOkCancelDialog(project,
                    "Plugin made changes which must be applied. Do you want to restart IntelliJ IDEA?",
                    "Community2Enterprise Plugin",
                    "Restart",
                    "Cancel",
                    Messages.getQuestionIcon());
            if (result == Messages.OK) {
                RestartDialogImpl.restartWithConfirmation();
            }
        } else {
            LOG.info("Version is the same. Do nothing");
        }
    }

    private String getPath() {
        PluginId pluginId = PluginId.getId(PLUGIN_ID);
        if (!PluginManagerCore.isPluginInstalled(pluginId)) {
            LOG.error("Plugin is not installed!");
            throw new RuntimeException(PLUGIN_ID + " plugin is not installed");
        }
        File pluginDirectory = PluginManagerCore.getPlugin(pluginId).getPath();
        if (pluginDirectory != null) {
            LOG.info("Plugin is located at: " + pluginDirectory.getAbsolutePath());
            return pluginDirectory.getAbsolutePath();
        } else {
            LOG.error("Plugin directory not found!");
            throw new RuntimeException(PLUGIN_ID + " does not have a plugin directory");
        }
    }

    private String getUrl(String jarName,
                          String packageName,
                          String buildNumber) {
        return INTELLIJ_RELEASE_URL + packageName + jarName + "/" + buildNumber + "/" + jarName + "-" + buildNumber + JAR;
    }

    private boolean checkFilesExist(String path, Map<String, String> jarUrls) {
        for (Map.Entry<String, String> jarUrl : jarUrls.entrySet()) {
            Path outputPath = getFilePath(path, jarUrl.getKey());
            if (!Files.exists(outputPath)) {
                return false;
            }
        }
        return true;
    }

    private void downloadJars(String path, Map<String, String> jarUrls) {
        for (Map.Entry<String, String> jarUrl : jarUrls.entrySet()) {
            try {
                URL url = URI.create(jarUrl.getValue()).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                try (InputStream inputStream = connection.getInputStream()) {
                    Path outputPath = getFilePath(path, jarUrl.getKey());
                    Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    LOG.info("Downloaded JAR: " + url.getFile());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to download JAR=" + jarUrl.getValue(), e);
            }
        }
    }

    private static @NotNull Path getFilePath(String path, String fileName) {
        return Paths.get(path + "/lib", new File(fileName + JAR).getName());
    }

    private Map<String, String> getUrls(String buildNumber) {
        Map<String, String> jarUrls = new HashMap<>();
        jarUrls.put(EXECUTION_PROCESS_ELEVATION, getUrl(EXECUTION_PROCESS_ELEVATION, EXECUTION_PACKAGE, buildNumber));
        jarUrls.put(EXECUTION_PROCESS_MEDIATOR_CLIENT, getUrl(EXECUTION_PROCESS_MEDIATOR_CLIENT, EXECUTION_PACKAGE, buildNumber));
        jarUrls.put(EXECUTION_EXECUTION_PROCESS_MEDIATOR_COMMON, getUrl(EXECUTION_EXECUTION_PROCESS_MEDIATOR_COMMON, EXECUTION_PACKAGE, buildNumber));
        jarUrls.put(EXECUTION_PROCESS_MEDIATOR_DAEMON, getUrl(EXECUTION_PROCESS_MEDIATOR_DAEMON, EXECUTION_PACKAGE, buildNumber));
        jarUrls.put(LIBRARIES_GRPC, getUrl(LIBRARIES_GRPC, LIBRARIES_PACKAGE, buildNumber));
        jarUrls.put(LIBRARIES_GRPC_NETTY_SHADED, getUrl(LIBRARIES_GRPC_NETTY_SHADED, LIBRARIES_PACKAGE, buildNumber));
        return jarUrls;
    }
}
