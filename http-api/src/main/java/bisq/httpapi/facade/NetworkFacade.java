package bisq.httpapi.facade;

import bisq.core.btc.nodes.BtcNodes;
import bisq.core.btc.setup.WalletsSetup;
import bisq.core.user.Preferences;

import bisq.network.p2p.NodeAddress;
import bisq.network.p2p.P2PService;
import bisq.network.p2p.network.Statistic;

import org.bitcoinj.core.Peer;

import javax.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;



import bisq.httpapi.model.BitcoinNetworkStatus;
import bisq.httpapi.model.P2PNetworkConnection;
import bisq.httpapi.model.P2PNetworkStatus;

public class NetworkFacade {

    private final P2PService p2PService;
    private final Preferences preferences;
    private final WalletsSetup walletsSetup;

    @Inject
    public NetworkFacade(P2PService p2PService,
                         bisq.core.user.Preferences preferences,
                         WalletsSetup walletsSetup) {
        this.p2PService = p2PService;
        this.preferences = preferences;
        this.walletsSetup = walletsSetup;
    }

    public P2PNetworkStatus getP2PNetworkStatus() {
        P2PNetworkStatus p2PNetworkStatus = new P2PNetworkStatus();
        NodeAddress address = p2PService.getAddress();
        if (address != null)
            p2PNetworkStatus.address = address.getFullAddress();
        p2PNetworkStatus.p2pNetworkConnection = p2PService.getNetworkNode().getAllConnections().stream()
                .map(P2PNetworkConnection::new)
                .collect(Collectors.toList());
        p2PNetworkStatus.totalSentBytes = Statistic.totalSentBytesProperty().get();
        p2PNetworkStatus.totalReceivedBytes = Statistic.totalReceivedBytesProperty().get();
        return p2PNetworkStatus;
    }

    public BitcoinNetworkStatus getBitcoinNetworkStatus() {
        BitcoinNetworkStatus networkStatus = new BitcoinNetworkStatus();
        List<Peer> peers = walletsSetup.connectedPeersProperty().get();
        if (peers != null)
            networkStatus.peers = peers.stream().map(peer -> peer.getAddress().toString()).collect(Collectors.toList());
        else
            networkStatus.peers = Collections.emptyList();
        networkStatus.useTorForBitcoinJ = preferences.getUseTorForBitcoinJ();
        networkStatus.bitcoinNodesOption = BtcNodes.BitcoinNodesOption.values()[preferences.getBitcoinNodesOptionOrdinal()];
        networkStatus.bitcoinNodes = preferences.getBitcoinNodes();
        return networkStatus;
    }
}
