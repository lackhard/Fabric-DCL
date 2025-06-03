package com.lei.config;

import ch.qos.logback.core.util.FileUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.file.PathUtils;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.stream.Stream;

/**
 *
 * @author zlt
 * @version 1.0
 * @date 2022/2/16
 * Blog: https://zlt2000.gitee.io
 * Github: https://github.com/zlt2000
 */
@Slf4j
@Configuration
@Data
public class GatewayConfig {
    /**
     * wallet文件夹路径
     */
    @Value("${fabric.walletDirectory}")
    private String walletDirectory;
    /**
     * 网络配置文件路径
     */
    @Value("${fabric.networkConfigPath}")
    private String networkConfigPath;
    /**
     * 用户证书路径
     */
    @Value("${fabric.certificatePath}")
    private String certificatePath;
    /**
     * 用户私钥路径
     */
    @Value("${fabric.privateKeyPath}")
    private String privateKeyPath;
    /**
     * 访问的组织名
     */
    @Value("${fabric.mspid}")
    private String mspid;
    /**
     * 用户名
     */
    @Value("${fabric.username}")
    private String username;
    /**
     * 通道名字
     */
    @Value("${fabric.channelName}")
    private String channelName;
    /**
     * 链码名字
     */
    @Value("${fabric.contractName}")
    private String contractName;

    /**
     * 连接网关
     */
    @Bean
    public Gateway connectGateway() throws IOException, InvalidKeyException, CertificateException {
        //使用org1中的user1初始化一个网关wallet账户用于连接网络
        Wallet wallet = Wallets.newFileSystemWallet(Paths.get(this.walletDirectory));
        X509Certificate certificate = readX509Certificate(Paths.get(this.certificatePath));
        PrivateKey privateKey = getPrivateKey(Paths.get(this.privateKeyPath));
        wallet.put(username, Identities.newX509Identity(this.mspid, certificate, privateKey));

        //根据connection.json 获取Fabric网络连接对象
        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, username)
                .networkConfig(Paths.get(this.networkConfigPath))
                .discovery(true);

        //连接网关
        return builder.connect();
    }

    /**
     * 获取网络
     */
    @Bean
    public Network network(Gateway gateway) {
        Network network = gateway.getNetwork(this.channelName);
       return network;
    }

    /**
     * 获取合约
     */
    @Bean
    public Contract contract(Network network) {
        return network.getContract(this.contractName);
    }

    /**
     * 获取 通道
     * @param network
     * @return
     */
    @Bean
    public Channel channel(Network network) {
        Channel channel = network.getChannel();

        return channel;
    }

    /**
     * 获取低级的 客户端
     * @param gateway
     * @return
     */
    @Bean
    public HFClient hfClient(Gateway gateway) {
        return gateway.getClient();
    }


    /**
     * 读取x509证书
     * @param certificatePath 证书路径
     * @return
     * @throws IOException
     * @throws CertificateException
     */
    private static X509Certificate readX509Certificate(final Path certificatePath) throws IOException, CertificateException {
        try (Reader certificateReader = Files.newBufferedReader(certificatePath, StandardCharsets.UTF_8)) {
            return Identities.readX509Certificate(certificateReader);
        }
    }

    /**
     * 读取私钥
     * @param privateKeyPath
     * @return
     * @throws IOException
     * @throws InvalidKeyException
     */
    private static PrivateKey getPrivateKey(final Path privateKeyPath) throws IOException, InvalidKeyException {
        Stream<Path> list = Files.list(privateKeyPath);
        Path path = list.findFirst().get();
        try (Reader privateKeyReader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return Identities.readPrivateKey(privateKeyReader);
        }
    }

}
