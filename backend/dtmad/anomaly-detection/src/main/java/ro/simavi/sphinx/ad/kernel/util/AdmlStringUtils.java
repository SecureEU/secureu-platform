package ro.simavi.sphinx.ad.kernel.util;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AdmlStringUtils {

    public String md5(String string) {
        try {
            return DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(string.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}
