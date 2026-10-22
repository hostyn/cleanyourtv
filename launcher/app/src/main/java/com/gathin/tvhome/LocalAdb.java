package com.gathin.tvhome;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * The smallest ADB client that runs one shell command on this TV's own adbd (network debugging, 127.0.0.1:5555),
 * for KeyBridge. Plain ADB protocol, no TLS: CNXN, AUTH with this launcher's own RSA key, OPEN "shell:", read until
 * CLSE. The first connection shows the TV's "Allow USB debugging?" prompt for that key; "Always allow" makes it
 * silent afterwards. The key lives in the app's private files and never leaves the TV.
 */
final class LocalAdb {
    private LocalAdb() { }

    static final String HOST = "127.0.0.1";
    static final int PORT = 5555;
    /** Long enough to walk to the TV and answer the prompt the first time. */
    private static final int AUTH_TIMEOUT_MS = 60_000;
    private static final int IO_TIMEOUT_MS = 15_000;

    private static final int CNXN = 0x4e584e43, AUTH = 0x48545541, OPEN = 0x4e45504f, OKAY = 0x59414b4f,
            CLSE = 0x45534c43, WRTE = 0x45545257, STLS = 0x534c5453;
    private static final int VERSION = 0x01000001, MAX_DATA = 256 * 1024;
    private static final int AUTH_TOKEN = 1, AUTH_SIGNATURE = 2, AUTH_RSAPUBLICKEY = 3;
    private static final int KEY_BITS = 2048;
    /** DER prefix of a SHA-1 DigestInfo: adbd checks the token as if it were a SHA-1 digest. */
    private static final byte[] SHA1_DIGEST_INFO = {0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02,
            0x1a, 0x05, 0x00, 0x04, 0x14};

    /** Runs {@code command} through adbd's shell and returns what it printed. */
    static String shell(File keyFile, String keyName, String command) throws IOException {
        KeyPair keys = loadOrCreateKey(keyFile);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 2_000);
            socket.setSoTimeout(IO_TIMEOUT_MS);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            send(out, CNXN, VERSION, MAX_DATA, "host::\0".getBytes(StandardCharsets.UTF_8));
            boolean signed = false, offeredKey = false;
            while (true) {
                Message m = read(in);
                if (m.command == CNXN) break;
                if (m.command == STLS) throw new IOException("adbd wants TLS (wireless debugging), not supported");
                if (m.command != AUTH || m.arg0 != AUTH_TOKEN) throw new IOException("unexpected " + m.command);
                if (!signed) {
                    send(out, AUTH, AUTH_SIGNATURE, 0, sign((RSAPrivateCrtKey) keys.getPrivate(), m.data));
                    signed = true;
                } else if (!offeredKey) {
                    // The key is new to this TV: adbd shows the prompt and answers CNXN once it is accepted.
                    send(out, AUTH, AUTH_RSAPUBLICKEY, 0, publicKeyBlob((RSAPublicKey) keys.getPublic(), keyName));
                    offeredKey = true;
                    socket.setSoTimeout(AUTH_TIMEOUT_MS);
                } else {
                    throw new IOException("key rejected");
                }
            }
            socket.setSoTimeout(IO_TIMEOUT_MS);
            int local = 1;
            send(out, OPEN, local, 0, ("shell:" + command + "\0").getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while (true) {
                Message m = read(in);
                if (m.command == WRTE) {
                    output.write(m.data);
                    send(out, OKAY, local, m.arg0, new byte[0]);
                } else if (m.command == CLSE) {
                    send(out, CLSE, local, m.arg0, new byte[0]);
                    break;
                } else if (m.command != OKAY) {
                    throw new IOException("unexpected " + m.command);
                }
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static final class Message {
        int command, arg0, arg1;
        byte[] data;
    }

    private static void send(OutputStream out, int command, int arg0, int arg1, byte[] data) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(24 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        int sum = 0;
        for (byte d : data) sum += d & 0xff;
        b.putInt(command).putInt(arg0).putInt(arg1).putInt(data.length).putInt(sum).putInt(~command).put(data);
        out.write(b.array());
        out.flush();
    }

    private static Message read(InputStream in) throws IOException {
        DataInputStream din = new DataInputStream(in);
        byte[] header = new byte[24];
        din.readFully(header);
        ByteBuffer b = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        Message m = new Message();
        m.command = b.getInt();
        m.arg0 = b.getInt();
        m.arg1 = b.getInt();
        int length = b.getInt();
        if (length < 0 || length > MAX_DATA) throw new IOException("bad length " + length);
        m.data = new byte[length];
        din.readFully(m.data);
        return m;
    }

    /** RSA PKCS#1 v1.5 signature of the 20-byte token, padded by hand as a SHA-1 DigestInfo (what adbd expects). */
    static byte[] sign(RSAPrivateCrtKey key, byte[] token) throws IOException {
        try {
            int size = key.getModulus().bitLength() / 8;
            byte[] block = new byte[size];
            block[1] = 1;
            int prefixAt = size - token.length - SHA1_DIGEST_INFO.length;
            for (int i = 2; i < prefixAt - 1; i++) block[i] = (byte) 0xff;
            System.arraycopy(SHA1_DIGEST_INFO, 0, block, prefixAt, SHA1_DIGEST_INFO.length);
            System.arraycopy(token, 0, block, size - token.length, token.length);
            Cipher rsa = Cipher.getInstance("RSA/ECB/NoPadding");
            rsa.init(Cipher.ENCRYPT_MODE, key);
            return rsa.doFinal(block);
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    /** The public key in adbd's own format (mincrypt RSAPublicKey, base64) plus a name, NUL-terminated. */
    static byte[] publicKeyBlob(RSAPublicKey key, String name) {
        int words = KEY_BITS / 32;
        BigInteger n = key.getModulus();
        BigInteger r32 = BigInteger.ONE.shiftLeft(32);
        BigInteger n0inv = n.mod(r32).modInverse(r32).negate().mod(r32);
        BigInteger rr = BigInteger.ONE.shiftLeft(KEY_BITS * 2).mod(n);
        ByteBuffer b = ByteBuffer.allocate(4 + 4 + words * 4 * 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(words).putInt(n0inv.intValue());
        putWords(b, n, words);
        putWords(b, rr, words);
        b.putInt(key.getPublicExponent().intValue());
        String text = Base64.getEncoder().encodeToString(b.array()) + " " + name + "\0";
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static void putWords(ByteBuffer b, BigInteger value, int words) {
        BigInteger mask = BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE);
        for (int i = 0; i < words; i++) b.putInt(value.shiftRight(32 * i).and(mask).intValue());
    }

    /** The launcher's ADB key, created on first use (PKCS#8 in the app's private files). */
    static KeyPair loadOrCreateKey(File file) throws IOException {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            if (file.isFile()) {
                RSAPrivateCrtKey priv = (RSAPrivateCrtKey) factory.generatePrivate(
                        new PKCS8EncodedKeySpec(Files.readAllBytes(file.toPath())));
                RSAPublicKey pub = (RSAPublicKey) factory.generatePublic(
                        new RSAPublicKeySpec(priv.getModulus(), priv.getPublicExponent()));
                return new KeyPair(pub, priv);
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_BITS);
            KeyPair pair = generator.generateKeyPair();
            File tmp = new File(file.getPath() + ".tmp");
            Files.write(tmp.toPath(), pair.getPrivate().getEncoded());
            if (!tmp.renameTo(file)) throw new IOException("cannot save " + file);
            return pair;
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }
}
