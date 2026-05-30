package com.company.bl.tools;

public final class BlCenterFlywayCli {

    private BlCenterFlywayCli() {
    }

    public static void main(String[] args) {
        int exitCode = execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int execute(String[] args) {
        return BlCenterFlywayCliSupport.execute(args);
    }
}
