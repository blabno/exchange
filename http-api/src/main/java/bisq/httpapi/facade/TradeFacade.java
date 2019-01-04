package bisq.httpapi.facade;

import bisq.core.trade.Trade;
import bisq.core.trade.TradeManager;

import javax.inject.Inject;

import javafx.collections.ObservableList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;



import bisq.httpapi.exceptions.NotFoundException;
import javax.validation.ValidationException;

public class TradeFacade {

    private final TradeManager tradeManager;

    @Inject
    public TradeFacade(TradeManager tradeManager) {
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
        Trade trade;
        try {
            trade = getTrade(tradeId);
        } catch (NotFoundException e) {
            return CompletableFuture.failedFuture(e);
        }

//        TODO BuyerStep3View does this: if (!trade.isPayoutPublished()) trade.setState(Trade.State.SELLER_CONFIRMED_IN_UI_FIAT_PAYMENT_RECEIPT);
//        TODO after that it delegates to PendingTradesDataModel which does some more checks and calls onFiatPaymentReceived on the trade
        if (!Trade.State.SELLER_RECEIVED_FIAT_PAYMENT_INITIATED_MSG.equals(trade.getState())) {
            return CompletableFuture.failedFuture(new ValidationException("Trade is not in the correct state to receive payment: " + trade.getState()));
        }
        return tradeManager.paymentReceived(trade);
    }

    public void moveFundsToBisqWallet(String tradeId) {
        Trade trade = getTrade(tradeId);
        tradeManager.moveFundsToBisqWallet(trade);
    }
}
