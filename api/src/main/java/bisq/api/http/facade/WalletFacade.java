package bisq.api.http.facade;

import bisq.api.http.model.WalletAddress;

import bisq.core.btc.model.AddressEntry;
import bisq.core.btc.wallet.BtcWalletService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.TransactionConfidence;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class WalletFacade {

    private final BtcWalletService btcWalletService;

    @Inject
    public WalletFacade(BtcWalletService btcWalletService) {
        this.btcWalletService = btcWalletService;
    }

    private static WalletAddress convertAddressEntryToWalletAddress(AddressEntry entry, BtcWalletService btcWalletService) {
        Coin balance;
        if (AddressEntry.Context.MULTI_SIG.equals(entry.getContext())) {
            balance = entry.getCoinLockedInMultiSig();
        } else {
            balance = btcWalletService.getBalanceForAddress(entry.getAddress());
        }
        TransactionConfidence confidence = btcWalletService.getConfidenceForAddress(entry.getAddress());
        int confirmations = confidence == null ? 0 : confidence.getDepthInBlocks();
        return new WalletAddress(entry.getAddressString(), balance.getValue(), confirmations, entry.getContext(), entry.getOfferId());
    }

    public WalletAddress getOrCreateAvailableUnusedWalletAddresses() {
        AddressEntry entry = btcWalletService.getFreshAddressEntry();
        return convertAddressEntryToWalletAddress(entry, btcWalletService);
    }

}
