package ro.simavi.sphinx.util;

public class OperatingSystemHelper {

    public static OperatingSystem getOperatingSystem() {

        // detecting the operating system using os.name System property
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        }

        else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return OperatingSystem.LINUX;
        }

        else if (os.contains("mac")) {
            return OperatingSystem.MAC;
        }

        else if (os.contains("sunos")) {
            return OperatingSystem.SOLARIS;
        }

        return null;
    }

}
