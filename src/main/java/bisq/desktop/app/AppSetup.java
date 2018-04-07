package bisq.desktop.app;

import bisq.core.app.BisqEnvironment;
import bisq.core.app.SetupUtils;

import bisq.network.crypto.EncryptionService;

import bisq.common.app.Version;
import bisq.common.crypto.KeyRing;
import bisq.common.proto.persistable.PersistedDataHost;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AppSetup {
    private final EncryptionService encryptionService;
    protected final KeyRing keyRing;
    ArrayList<PersistedDataHost> persistedDataHosts = new ArrayList<>();
    private CompletableFuture<Void> checkCryptoSetupResult;
    private CompletableFuture<Void> initPersistedDataHostsResult;
    private CompletableFuture<Void> initBasicServicesResult;

    @Inject
    public AppSetup(EncryptionService encryptionService,
                    KeyRing keyRing) {
        // we need to reference it so the seed node stores tradeStatistics
        this.encryptionService = encryptionService;
        this.keyRing = keyRing;

        Version.setBaseCryptoNetworkId(BisqEnvironment.getBaseCurrencyNetwork().ordinal());
        Version.printVersion();
    }

    private CompletableFuture<Void> checkCryptoSetup() {
        if (null != checkCryptoSetupResult)
            return checkCryptoSetupResult;

        checkCryptoSetupResult = new CompletableFuture<>();

        SetupUtils.checkCryptoSetup(keyRing, encryptionService, () -> checkCryptoSetupResult.complete(null), throwable -> {
            log.error(throwable.getMessage());
            throwable.printStackTrace();
            checkCryptoSetupResult.completeExceptionally(throwable);
            System.exit(1);
        });

        return checkCryptoSetupResult;
    }

    public CompletableFuture<Void> initPersistedDataHosts() {
        if (null != initPersistedDataHostsResult)
            return initPersistedDataHostsResult;
        initPersistedDataHostsResult = checkCryptoSetup()
                .thenRun(this::doInitPersistedDataHosts);
        return initPersistedDataHostsResult;
    }

    private void doInitPersistedDataHosts() {
        log.info("doInitPersistedDataHosts");
        persistedDataHosts.forEach(e -> {
            try {
                log.info("call readPersisted at " + e.getClass().getSimpleName());
                e.readPersisted();
            } catch (Throwable e1) {
                log.error("readPersisted error", e1);
            }
        });
    }

    public CompletableFuture<Void> initBasicServices() {
        if (null != initBasicServicesResult)
            return initBasicServicesResult;
        initBasicServicesResult = initPersistedDataHosts()
                .thenApply(r -> this.doInitBasicServices())
                .thenRun(this::onBasicServicesInitialized);
        return initBasicServicesResult;
    }

    protected abstract <U> U doInitBasicServices();

    protected abstract void onBasicServicesInitialized();

}
