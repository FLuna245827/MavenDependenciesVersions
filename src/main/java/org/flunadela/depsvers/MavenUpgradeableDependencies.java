package org.flunadela.depsvers;

import picocli.CommandLine;

public class MavenUpgradeableDependencies implements Runnable {
    @CommandLine.Parameters(index = "0", description = "Path to the Maven POM file to analyze")
    private String pomFilePath;

    @CommandLine.Option(names = {"--showOnlyLatestVersion", "-olv"}, description = "Flag to show only the latest available version for each dependency")
    private boolean showOnlyLatestVersion = false;

    @CommandLine.Option(names = {"--useSettingsXml", "-s"}, description = "Flag to use the settings.xml file from the Maven configuration")
    private boolean useSettingsXml = false;

    public void run() {
        MavenDepsScanner depsScanner = new MavenDepsScanner();
        depsScanner.scan(pomFilePath, useSettingsXml, showOnlyLatestVersion);
    }

    public static void main(String[] args) {
        new CommandLine(new MavenUpgradeableDependencies()).execute(args);
    }
}
