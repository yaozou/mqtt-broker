package com.yao.broker.core.handler;

import com.yao.broker.core.config.NettyConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * @Description: ssl认证
 *<ul>
 *     <li>netty通过JDK的SSLEngine，以SslHandler的方式提供对SSL/TLS安全传输的支持</li>
 *</ul>
 *
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Service
public class SslOperatorHandler {
    private static final String PROTOCOL = "TLS";
    @Autowired
    private NettyConfig nettyConfig;

    public ChannelHandler createSsl(SocketChannel channel) throws Exception{
        /*SslContext sslContext = createSslContext();
        SSLEngine engine = sslContext.newEngine(
                channel.alloc(),
                channel.remoteAddress().getHostString(),
                channel.remoteAddress().getPort());*/

        SSLEngine engine = creatSslEngine();

        engine.setUseClientMode(false);
        // 默认为双向认证
        boolean needsClientAuth = nettyConfig.isNeedsClientAuth();
        engine.setNeedClientAuth(needsClientAuth);
        return new SslHandler(engine);
    }

    private SSLEngine creatSslEngine() throws Exception {
        KeyStore keyStore = getKeyStore();
        final KeyManagerFactory kmf = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore,nettyConfig.getKeyManagerPassword().toCharArray());

        final TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
        sslContext.init(kmf.getKeyManagers(),tmf.getTrustManagers(),null);

        return sslContext.createSSLEngine();
    }

    private SslContext createSslContext( )throws Exception{
        String keyPwd = nettyConfig.getKeyManagerPassword();
        KeyStore keyStore = getKeyStore();
        SslContextBuilder contextBuilder;

        SslProvider sslProvider = SslProvider.valueOf(nettyConfig.getSslProvider());
        switch (sslProvider){
            case JDK:
                contextBuilder = builderWithJdkProvider(keyStore,keyPwd);
                break;
            case OPENSSL:
            case OPENSSL_REFCNT:
                contextBuilder = builderWithOpenSSLProvider(keyStore,keyPwd);
                break;
            default:
                return null;
        }

        contextBuilder.sslProvider(sslProvider);
        SslContext sslContext = contextBuilder.build();
        return sslContext;
    }

    private KeyStore getKeyStore() throws Exception{
        final String jksPath = nettyConfig.getJksPath();
        final String keyStorePassword = nettyConfig.getKeyStorePassword();

        String keyType = "jks";
        final KeyStore keyStore = KeyStore.getInstance(keyType);
        try (InputStream jksInputStream = jksDatastore(jksPath)) {
            keyStore.load(jksInputStream, keyStorePassword.toCharArray());
        }
        return keyStore;
    }

    private InputStream jksDatastore(String jksPath) throws Exception {
        URL jksUrl = getClass().getClassLoader().getResource(jksPath);
        if (jksUrl != null) {
            return getClass().getClassLoader().getResourceAsStream(jksPath);
        }
        File jksFile = new File(jksPath);
        if (jksFile.exists()) {
            return new FileInputStream(jksFile);
        }

        throw new FileNotFoundException(
                "The keystore file does not exist. Url = " + jksFile.getAbsolutePath());
    }

    private static SslContextBuilder builderWithJdkProvider(KeyStore ks, String keyPassword)
            throws GeneralSecurityException {
        final KeyManagerFactory kmf = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyPassword.toCharArray());

        return SslContextBuilder.forServer(kmf);
    }

    private static SslContextBuilder builderWithOpenSSLProvider(KeyStore ks, String keyPassword)
            throws GeneralSecurityException {
        for (String alias : Collections.list(ks.aliases())) {
            if (ks.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                PrivateKey key = (PrivateKey) ks.getKey(alias, keyPassword.toCharArray());
                Certificate[] chain = ks.getCertificateChain(alias);
                X509Certificate[] certChain = new X509Certificate[chain.length];
                System.arraycopy(chain, 0, certChain, 0, chain.length);
                return SslContextBuilder.forServer(key, certChain);
            }
        }
        throw new KeyManagementException("the SSL key-store does not contain a private key");
    }
}
