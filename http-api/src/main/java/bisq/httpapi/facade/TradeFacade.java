package bisq.httpapi.facade;

import bisq.core.btc.model.AddressEntry;
import bisq.core.btc.wallet.BtcWalletService;
import bisq.core.trade.Trade;
import bisq.core.trade.TradeManager;
import bisq.core.trade.protocol.SellerAsMakerProtocol;
import bisq.core.trade.protocol.SellerAsTakerProtocol;
import bisq.core.trade.protocol.TradeProtocol;

import bisq.common.handlers.ErrorMessageHandler;
import bisq.common.handlers.ResultHandler;

import javax.inject.Inject;

import javafx.collections.ObservableList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.httpapi.facade.FacadeUtil.failFuture;



import bisq.httpapi.exceptions.NotFoundException;
import javax.validation.ValidationException;

public class TradeFacade {

    private final BtcWalletService btcWalletService;
    private final TradeManager tradeManager;

    @Inject
    public TradeFacade(BtcWalletService btcWalletService, TradeManager tradeManager) {
        this.btcWalletService = btcWalletService;
        this.tradeManager = tradeManager;
    }

    public List<Trade> getTradeList() {
        ObservableList<Trade> tradableList = tradeManager.getTradableList();
        if (tradableList != null) return tradableList.sorted();
        return Collections.emptyList();
    }

    public Trade getTrade(String tradeId) {
        String safeTradeId = (tradeId == null) ? "" : tradeId;
        Optional<Trade> tradeOptional = getTradeList().stream().filter(item -> safeTradeId.equals(item.getId())).findAny();
        if (!tradeOptional.isPresent()) {
            throw new NotFoundException("Trade not found: " + tradeId);
        }
        return tradeOptional.get();
    }

    public CompletableFuture<Void> paymentStarted(String tradeId) {
        Trade trade;
        try {
            trade = getTrade(tradeId);
        } catch (NotFoundException e) {
            return CompletableFuture.failedFuture(e);
        }

//        TODO BuyerStep2View does this:  if (trade.isFiatSent()) trade.setState(Trade.State.DEPOSIT_CONFIRMED_IN_BLOCK_CHAIN)
//        TODO after that it delegates to PendingTradesDataModel which does some more checks and calls onFiatPaymentStarted on the trade
        if (!Trade.State.DEPOSIT_CONFIRMED_IN_BLOCK_CHAIN.equals(trade.getState())) {
            return CompletableFuture.failedFuture(new ValidationException("Trade is not in the correct state to start payment: " + trade.getState()));
        }
        return tradeManager.paymentStarted(trade);
    }

    public CompletableFuture<Void> paymentReceived(String tradeId) {
        CompletableFuture<Void> futureResult = new CompletableFuture<>();
        Trade trade;
        try {
            trade = getTrade(tradeId);
        } catch (NotFoundException e) {
            return failFuture(futureResult, e);
        }

        if (!Trade.State.SELLER_RECEIVED_FIAT_PAYMENT_INITIATED_MSG.equals(trade.getState())) {
            return failFuture(futureResult, new ValidationException("Trade is not in the correct state to receive payment: " + trade.getState()));
        }
        TradeProtocol tradeProtocol = trade.getTradeProtocol();

        if (!(tradeProtocol instanceof SellerAsTakerProtocol || tradeProtocol instanceof SellerAsMakerProtocol)) {
            return failFuture(futureResult, new ValidationException("Trade is not in the correct state to receive payment: " + trade.getState()));
        }

        ResultHandler resultHandler = () -> futureResult.complete(null);
        ErrorMessageHandler errorResultHandler = message -> futureResult.completeExceptionally(new RuntimeException(message));

        if (tradeProtocol instanceof SellerAsMakerProtocol) {
            ((SellerAsMakerProtocol) tradeProtocol).onFiatPaymentReceived(resultHandler, errorResultHandler);
        } else {
            ((SellerAsTakerProtocol) tradeProtocol).onFiatPaymentReceived(resultHandler, errorResultHandler);
        }
        return futureResult;
    }

    public void moveFundsToBisqWallet(String tradeId) {
        Trade trade = getTrade(tradeId);
        Trade.State tradeState = trade.getState();
        if (!Trade.State.SELLER_SAW_ARRIVED_PAYOUT_TX_PUBLISHED_MSG.equals(tradeState) && !Trade.State.BUYER_RECEIVED_PAYOUT_TX_PUBLISHED_MSG.equals(tradeState))
            throw new ValidationException("Trade is not in the correct state to transfer funds out: " + tradeState);
        btcWalletService.swapTradeEntryToAvailableEntry(trade.getId(), AddressEntry.Context.TRADE_PAYOUT);
        // TODO do we need to handle this ui stuff? --> handleTradeCompleted();
        tradeManager.addTradeToClosedTrades(trade);
    }
}
