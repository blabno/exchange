package bisq.httpapi.facade;

import bisq.core.btc.BalanceUtil;
import bisq.core.btc.exceptions.AddressEntryException;
import bisq.core.btc.exceptions.InsufficientFundsException;
import bisq.core.btc.model.AddressEntry;
import bisq.core.btc.setup.WalletsSetup;
import bisq.core.btc.wallet.BtcWalletService;
import bisq.core.btc.wallet.WalletService;
import bisq.core.btc.wallet.WalletsManager;
import bisq.core.locale.Res;
import bisq.core.trade.Trade;
import bisq.core.trade.TradeManager;
import bisq.core.util.validation.BtcAddressValidator;

import bisq.common.storage.FileUtil;
import bisq.common.storage.Storage;
import bisq.common.util.Tuple2;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.util.concurrent.FutureCallback;

import org.spongycastle.crypto.params.KeyParameter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

import static bisq.httpapi.facade.FacadeUtil.failFuture;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;



import bisq.httpapi.exceptions.AmountTooLowException;
import bisq.httpapi.exceptions.UnauthorizedException;
import bisq.httpapi.model.SeedWords;
import bisq.httpapi.model.WalletAddress;
import bisq.httpapi.model.WalletAddressList;
import bisq.httpapi.model.WalletTransaction;
import bisq.httpapi.model.WalletTransactionList;
import javax.validation.ValidationException;

@Slf4j
public class WalletFacade {

    private final BtcWalletService btcWalletService;
    private final TradeManager tradeManager;
    private final WalletsSetup walletsSetup;
    private final WalletsManager walletsManager;
    private final BalanceUtil balanceUtil;
    private final ShutdownFacade shutdownFacade;
    private final File storageDir;

    @Inject
    public WalletFacade(BtcWalletService btcWalletService,
                        TradeManager tradeManager,
                        WalletsSetup walletsSetup,
                        WalletsManager walletsManager,
                        BalanceUtil balanceUtil,
                        ShutdownFacade shutdownFacade,
                        @Named(Storage.STORAGE_DIR) File storageDir) {
        this.btcWalletService = btcWalletService;
        this.tradeManager = tradeManager;
        this.walletsSetup = walletsSetup;
        this.walletsManager = walletsManager;
        this.balanceUtil = balanceUtil;
        this.shutdownFacade = shutdownFacade;
        this.storageDir = storageDir;
    }

    public WalletTransactionList getWalletTransactions() {
        Wallet wallet = walletsSetup.getBtcWallet();
        WalletTransactionList walletTransactions = new WalletTransactionList();
        walletTransactions.transactions.addAll(btcWalletService.getTransactions(true)
                .stream()
                .map(transaction -> toWalletTransaction(wallet, transaction))
                .collect(Collectors.toList()));
        walletTransactions.total = walletTransactions.transactions.size();
        return walletTransactions;
    }

    private WalletTransaction toWalletTransaction(Wallet wallet, Transaction transaction) {
        Coin valueSentFromMe = transaction.getValueSentFromMe(wallet);
        Coin valueSentToMe = transaction.getValueSentToMe(wallet);
        boolean received = false;
        String addressString = null;

        if (valueSentToMe.isZero()) {
            for (TransactionOutput output : transaction.getOutputs()) {
                if (!btcWalletService.isTransactionOutputMine(output)) {
                    if (WalletService.isOutputScriptConvertibleToAddress(output)) {
                        addressString = WalletService.getAddressStringFromOutput(output);
                        break;
                    }
                }
            }
        } else if (valueSentFromMe.isZero()) {
            received = true;
            for (TransactionOutput output : transaction.getOutputs()) {
                if (btcWalletService.isTransactionOutputMine(output) &&
                        WalletService.isOutputScriptConvertibleToAddress(output)) {
                    addressString = WalletService.getAddressStringFromOutput(output);
                    break;
                }
            }
        } else {
            for (TransactionOutput output : transaction.getOutputs()) {
                if (!btcWalletService.isTransactionOutputMine(output)) {
                    if (WalletService.isOutputScriptConvertibleToAddress(output)) {
                        addressString = WalletService.getAddressStringFromOutput(output);
                        break;
                    }
                }
            }
        }
        TransactionConfidence confidence = transaction.getConfidence();
        int confirmations = confidence == null ? 0 : confidence.getDepthInBlocks();

        WalletTransaction walletTransaction = new WalletTransaction();
        walletTransaction.updateTime = transaction.getUpdateTime().getTime();
        walletTransaction.hash = transaction.getHashAsString();
        walletTransaction.fee = (transaction.getFee() == null) ? -1 : transaction.getFee().value;
        walletTransaction.value = transaction.getValue(wallet).value;
        walletTransaction.valueSentFromMe = valueSentFromMe.value;
        walletTransaction.valueSentToMe = valueSentToMe.value;
        walletTransaction.confirmations = confirmations;
        walletTransaction.inbound = received;
        walletTransaction.address = addressString;
        return walletTransaction;
    }

    public WalletAddressList getWalletAddresses(WalletAddressPurpose purpose) {
        Stream<AddressEntry> addressEntryStream;
        if (WalletAddressPurpose.SEND_FUNDS.equals(purpose)) {
            addressEntryStream = balanceUtil.getAddressEntriesForAvailableFunds();
        } else if (WalletAddressPurpose.RESERVED_FUNDS.equals(purpose)) {
            addressEntryStream = balanceUtil.getAddressEntriesForReservedFunds();
        } else if (WalletAddressPurpose.LOCKED_FUNDS.equals(purpose)) {
            addressEntryStream = balanceUtil.getAddressEntriesForLockedFunds();
        } else if (WalletAddressPurpose.RECEIVE_FUNDS.equals(purpose)) {
            addressEntryStream = btcWalletService.getAvailableAddressEntries().stream();
        } else {
            addressEntryStream = btcWalletService.getAddressEntryListAsImmutableList().stream();
        }
        List<WalletAddress> walletAddresses = addressEntryStream
                .map(entry -> convertAddressEntryToWalletAddress(entry, btcWalletService))
                .collect(toList());
        WalletAddressList walletAddressList = new WalletAddressList();
        walletAddressList.walletAddresses = walletAddresses;
        walletAddressList.total = walletAddresses.size();
        return walletAddressList;
    }

    public void withdrawFunds(Set<String> sourceAddresses, Coin amountAsCoin, boolean feeExcluded, String targetAddress)
            throws AddressEntryException, InsufficientFundsException, AmountTooLowException {
        // get all address entries
        List<AddressEntry> sourceAddressEntries = sourceAddresses.stream()
                .filter(Objects::nonNull)
                .map(address -> btcWalletService.getAddressEntryListAsImmutableList().stream().filter(addressEntry -> address.equals(addressEntry.getAddressString())).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // this filter matches all unauthorized address types
        Predicate<AddressEntry> filterNotAllowedAddressEntries = addressEntry -> !(AddressEntry.Context.AVAILABLE.equals(addressEntry.getContext())
                || AddressEntry.Context.TRADE_PAYOUT.equals(addressEntry.getContext()));
        // check if there are any unauthorized address types
        if (sourceAddressEntries.stream().anyMatch(filterNotAllowedAddressEntries)) {
            throw new ValidationException("Funds can be withdrawn only from addresses with context AVAILABLE and TRADE_PAYOUT");
        }

        Coin sendersAmount;
        // We do not know sendersAmount if senderPaysFee is true. We repeat fee calculation after first attempt if senderPaysFee is true.
        Transaction feeEstimationTransaction;
        try {
            feeEstimationTransaction = btcWalletService.getFeeEstimationTransactionForMultipleAddresses(sourceAddresses, amountAsCoin);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("dust limit")) {
                throw new AmountTooLowException(e.getMessage());
            }
            throw e;
        }
        if (feeExcluded && feeEstimationTransaction != null) {
            sendersAmount = amountAsCoin.add(feeEstimationTransaction.getFee());
            feeEstimationTransaction = btcWalletService.getFeeEstimationTransactionForMultipleAddresses(sourceAddresses, sendersAmount);
        }
        checkNotNull(feeEstimationTransaction, "feeEstimationTransaction must not be null");
        Coin fee = feeEstimationTransaction.getFee();
        sendersAmount = feeExcluded ? amountAsCoin.add(fee) : amountAsCoin;
        Coin receiverAmount = feeExcluded ? amountAsCoin : amountAsCoin.subtract(fee);

        Coin totalAvailableAmountOfSelectedItems = sourceAddressEntries.stream()
                .map(address -> btcWalletService.getBalanceForAddress(address.getAddress()))
                .reduce(Coin.ZERO, Coin::add);

        if (!sendersAmount.isPositive())
            throw new ValidationException("Senders amount must be positive");
        if (!new BtcAddressValidator().validate(targetAddress).isValid)
            throw new ValidationException("Invalid target address");
        if (sourceAddresses.isEmpty())
            throw new ValidationException("List of source addresses must not be empty");
        if (sendersAmount.compareTo(totalAvailableAmountOfSelectedItems) > 0)
            throw new InsufficientFundsException("Not enough funds in selected addresses");

        if (receiverAmount.isPositive()) {
            try {
                //                TODO return completable future
                btcWalletService.sendFundsForMultipleAddresses(sourceAddresses, targetAddress, amountAsCoin, fee, null, null, new FutureCallback<>() {
                    @Override
                    public void onSuccess(@javax.annotation.Nullable Transaction transaction) {
                        if (transaction != null) {
                            log.debug("onWithdraw onSuccess tx ID:" + transaction.getHashAsString());
                        } else {
                            log.error("onWithdraw transaction is null");
                        }

                        List<Trade> trades = new ArrayList<>(tradeManager.getTradableList());
                        trades.stream()
                                .filter(Trade::isPayoutPublished)
                                .forEach(trade -> btcWalletService.getAddressEntry(trade.getId(), AddressEntry.Context.TRADE_PAYOUT)
                                        .ifPresent(addressEntry -> {
                                            if (btcWalletService.getBalanceForAddress(addressEntry.getAddress()).isZero())
                                                tradeManager.addTradeToClosedTrades(trade);
                                        }));
                    }

                    @Override
                    public void onFailure(@Nonnull Throwable t) {
                        log.error("onWithdraw onFailure");
                    }
                });
            } catch (org.bitcoinj.core.InsufficientMoneyException e) {
                throw new InsufficientFundsException(e.getMessage());
            }
        } else {
            throw new AmountTooLowException(Res.get("portfolio.pending.step5_buyer.amountTooLow"));
        }
    }

    public WalletAddress getOrCreateAvailableUnusedWalletAddresses() {
        AddressEntry entry = btcWalletService.getFreshAddressEntry();
        return convertAddressEntryToWalletAddress(entry, btcWalletService);
    }

    public CompletableFuture<Void> restoreWalletFromSeedWords(List<String> mnemonicCode, String walletCreationDate, String password) {
        if (btcWalletService.isEncrypted() && (password == null || !isWalletPasswordValid(password)))
            throw new UnauthorizedException();
        CompletableFuture<Void> futureResult = new CompletableFuture<>();
        long date = walletCreationDate != null ? LocalDate.parse(walletCreationDate).atStartOfDay().toEpochSecond(ZoneOffset.UTC) : 0;
        DeterministicSeed seed = new DeterministicSeed(mnemonicCode, null, "", date);
        //        TODO this logic comes from GUIUtils

        try {
            FileUtil.renameFile(new File(storageDir, "AddressEntryList"), new File(storageDir, "AddressEntryList_wallet_restore_" + System.currentTimeMillis()));
        } catch (Throwable t) {
            return failFuture(futureResult, t);
        }
        walletsManager.restoreSeedWords(
                seed,
                () -> futureResult.complete(null),
                throwable -> failFuture(futureResult, throwable));
        futureResult.thenRunAsync(shutdownFacade::shutDown);
        return futureResult;
    }

    public SeedWords getSeedWords(String password) {
        DeterministicSeed keyChainSeed = btcWalletService.getKeyChainSeed();
        LocalDate walletCreationDate = Instant.ofEpochSecond(walletsManager.getChainSeedCreationTimeSeconds()).atZone(ZoneId.systemDefault()).toLocalDate();

        DeterministicSeed seed = keyChainSeed;
        if (keyChainSeed.isEncrypted()) {
            if (password == null)
                throw new UnauthorizedException();
            KeyParameter aesKey = getAESKey(password);
            if (!isWalletPasswordValid(aesKey))
                throw new UnauthorizedException();
            seed = walletsManager.getDecryptedSeed(aesKey, btcWalletService.getKeyChainSeed(), btcWalletService.getKeyCrypter());
        }

        return new SeedWords(seed.getMnemonicCode(), walletCreationDate.toString());
    }

    public enum WalletAddressPurpose {
        LOCKED_FUNDS,
        RECEIVE_FUNDS,
        RESERVED_FUNDS,
        SEND_FUNDS
    }

    KeyParameter getAESKey(String password) {
        return getAESKeyAndScrypt(password).first;
    }

    Tuple2<KeyParameter, KeyCrypterScrypt> getAESKeyAndScrypt(String password) {
        KeyCrypterScrypt keyCrypterScrypt = walletsManager.getKeyCrypterScrypt();
        return new Tuple2<>(keyCrypterScrypt.deriveKey(password), keyCrypterScrypt);
    }

    boolean isWalletPasswordValid(String password) {
        KeyParameter aesKey = getAESKey(password);
        return isWalletPasswordValid(aesKey);
    }

    boolean isWalletPasswordValid(KeyParameter aesKey) {
        return aesKey != null && walletsManager.checkAESKey(aesKey);
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
}