package me.xuqu.palmx.registry;

import lombok.extern.slf4j.Slf4j;
import me.xuqu.palmx.common.PalmxConfig;
import me.xuqu.palmx.qos.QoSHandler;
import me.xuqu.palmx.util.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static me.xuqu.palmx.serialize.Serialization.serialize;

@Slf4j
public class ZookeeperUpdater {

    private static CuratorFramework client = CuratorUtils.getClient();
    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public static void startUpdating(String path, String newData) {

    }

    public static void startUpdating() {
        // 获取所以服务
        Set<String> services = ServiceRegistry.services;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("正在更新当前主机的节点信息");
                int localQoSLevel = QoSHandler.getLocalQoSLevel();
                // 遍历服务
                for (String service : services) {
                    // 根据服务获取节点内容
                    // 构建注册节点对象
                    int serializationType = PalmxConfig.getSerializationType().ordinal();
                    RegistryDTO registryDTO = new RegistryDTO();
                    registryDTO.setProtocol("http3");
                    registryDTO.setHost("serviceAddress");
                    registryDTO.setPort(8080);
                    registryDTO.setServiceName(service);
                    // 获取本地的数据指标对象
                    registryDTO.setQoSLevel(localQoSLevel);
                    registryDTO.setTmRefresh(new Date());
                    byte[] registryDTOByte = serialize((byte) serializationType, registryDTO);
                    // 得到更新QoS与刷新时间
                    updateZNode(service, registryDTOByte);
                }
            } catch (Exception e) {
                log.warn("update zk err, msg = {}", e.getMessage());
                e.printStackTrace(); // 实际应用中应记录到日志系统
            }
        }, 0, 10, TimeUnit.SECONDS); // 每 10 秒更新一次

    }

    private static void updateZNode(String path, byte[] dataBytes) throws Exception {
        if (client.checkExists().forPath(path) != null) {
            client.setData().forPath(path, dataBytes);
        } else {
            client.create().creatingParentsIfNeeded().forPath(path, dataBytes);
        }
        log.info("Updated node {} with data: {}", path, dataBytes);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
