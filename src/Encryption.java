import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encryption {
	public static String sha256(String origin) {

		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte[] byteHash = digest.digest(origin.getBytes(StandardCharsets.UTF_8));

		StringBuffer hexHash = new StringBuffer();
		for (int i = 0; i < byteHash.length; i++) {
			String hexDigit = Integer.toHexString(0xff & byteHash[i]);
			if (hexDigit.length() == 1) {
				hexHash.append('0');
			}
			hexHash.append(hexDigit);
		}
		return hexHash.toString();
	}
}
