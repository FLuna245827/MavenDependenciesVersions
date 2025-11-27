package org.flunadela.depsvers;

public class MavenUpgradeableDependencies {

    public static void main(String[] args) {
        boolean showOnlyLatestArg = false;

        if (args.length == 2) {
            showOnlyLatestArg = "showOnlyLatestVersion".equals(args[1]);
        }

        MavenDepsScanner depsScanner = new MavenDepsScanner();
        depsScanner.scan(args[0], showOnlyLatestArg);
    }
}
