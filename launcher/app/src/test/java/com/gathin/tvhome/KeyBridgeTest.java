package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

public class KeyBridgeTest {

    @Test
    public void scriptMatchesKeyPressesByHexCode() {
        Map<Integer, String> commands = new TreeMap<>();
        commands.put(566, "am start -n com.netflix.ninja/.MainActivity");
        commands.put(752, "am start -a android.settings.SETTINGS");
        String script = KeyBridge.script(commands);
        assertTrue(script.startsWith("#!/system/bin/sh\n"));
        assertTrue(script.contains("echo $$ > " + KeyBridge.PID));
        assertTrue(script.contains("[ \"$type\" = 0001 ] && [ \"$value\" = 00000001 ] || continue"));
        assertTrue(script.contains("    0236) am start -n com.netflix.ninja/.MainActivity >/dev/null 2>&1 ;;\n"));
        assertTrue(script.contains("    02f0) am start -a android.settings.SETTINGS >/dev/null 2>&1 ;;\n"));
    }

    @Test
    public void startCommandCarriesTheScriptInBase64() {
        String script = KeyBridge.script(new TreeMap<>());
        String command = KeyBridge.startCommand(script);
        String b64 = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_8));
        assertTrue(command.startsWith("echo " + b64 + " | base64 -d > " + KeyBridge.SCRIPT + ".new; "));
        assertTrue(command.contains("(setsid nohup " + KeyBridge.SCRIPT + " >/dev/null 2>&1 </dev/null &)"));
        assertTrue(command.endsWith("then echo started; else echo failed; fi; fi"));
    }

    @Test
    public void signatureIsPkcs1OverSha1DigestInfo() throws Exception {
        KeyPair pair = rsa();
        byte[] token = new byte[20];
        for (int i = 0; i < token.length; i++) token[i] = (byte) (i * 7 + 1);
        byte[] signature = LocalAdb.sign((RSAPrivateCrtKey) pair.getPrivate(), token);
        // NONEwithRSA verifies PKCS#1 v1.5 padding around exactly the bytes given: DigestInfo prefix + token.
        byte[] digestInfo = {0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};
        byte[] signed = new byte[digestInfo.length + token.length];
        System.arraycopy(digestInfo, 0, signed, 0, digestInfo.length);
        System.arraycopy(token, 0, signed, digestInfo.length, token.length);
        Signature verifier = Signature.getInstance("NONEwithRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(signed);
        assertTrue(verifier.verify(signature));
    }

    @Test
    public void publicKeyBlobIsMincryptFormat() throws Exception {
        RSAPublicKey key = (RSAPublicKey) rsa().getPublic();
        String text = new String(LocalAdb.publicKeyBlob(key, "cleanyourtv@test"), StandardCharsets.UTF_8);
        assertTrue(text.endsWith(" cleanyourtv@test\0"));
        byte[] raw = Base64.getDecoder().decode(text.substring(0, text.indexOf(' ')));
        assertEquals(4 + 4 + 256 + 256 + 4, raw.length);
        ByteBuffer b = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(64, b.getInt());
        int n0inv = b.getInt();
        BigInteger n = key.getModulus();
        // n0inv * n[0] == -1 mod 2^32
        assertEquals(-1, n0inv * n.intValue());
        byte[] modulus = new byte[256];
        b.get(modulus);
        byte[] bigEndian = new byte[256];
        for (int i = 0; i < 256; i++) bigEndian[i] = modulus[255 - i];
        assertArrayEquals(n.toByteArray().length == 257 ? java.util.Arrays.copyOfRange(n.toByteArray(), 1, 257)
                : n.toByteArray(), bigEndian);
        b.position(4 + 4 + 256 + 256);
        assertEquals(65537, b.getInt());
    }

    private static KeyPair rsa() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
