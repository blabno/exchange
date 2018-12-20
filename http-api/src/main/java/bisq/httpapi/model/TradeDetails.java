package bisq.httpapi.model;

import bisq.core.offer.Offer;
import bisq.core.trade.Contract;
import bisq.core.trade.Trade;

import bisq.network.p2p.NodeAddress;

import com.fasterxml.jackson.annotation.JsonInclude;



import bisq.httpapi.model.payment.PaymentAccount;
import bisq.httpapi.model.payment.PaymentAccountHelper;

@SuppressWarnings("WeakerAccess")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TradeDetails {

    public PaymentAccount buyerPaymentAccount;
    public PaymentAccount sellerPaymentAccount;
    public String id;
    public OfferDetail offer;
    public boolean isCurrencyForTakerFeeBtc;
    public long txFee;
    public long takerFee;
    public long takeOfferDate;
    public String takerFeeTxId;
    public String depositTxId;
    public String payoutTxId;
    public long tradeAmount;
    public long tradePrice;
    public Trade.State state;
    public Trade.DisputeState disputeState;
    public Trade.TradePeriodState tradePeriodState;
    public byte[] arbitratorBtcPubKey;
    public byte[] contractHash;
    public String mediatorNodeAddress;
    public String takerContractSignature;
    public String makerContractSignature;
    public String arbitratorNodeAddress;
    public String tradingPeerNodeAddress;
    public String takerPaymentAccountId;
    public String errorMessage;
    public String counterCurrencyTxId;

    public TradeDetails(Trade trade) {
        this.id = trade.getId();
        Offer offer = trade.getOffer();
        if (offer != null)
            this.offer = new OfferDetail(offer);
        Contract contract = trade.getContract();
        if (contract != null) {
            this.buyerPaymentAccount = PaymentAccountHelper.toRestModel(contract.getBuyerPaymentAccountPayload());
            this.sellerPaymentAccount = PaymentAccountHelper.toRestModel(contract.getSellerPaymentAccountPayload());
        }
        this.isCurrencyForTakerFeeBtc = trade.isCurrencyForTakerFeeBtc();
        this.txFee = trade.getTxFeeAsLong();
        this.takerFee = trade.getTakerFeeAsLong();
        this.takeOfferDate = trade.getTakeOfferDate().getTime();
        this.takerFeeTxId = trade.getTakerFeeTxId();
        this.depositTxId = trade.getDepositTxId();
        this.payoutTxId = trade.getPayoutTxId();
        this.tradeAmount = trade.getTradeAmountAsLong();
        this.tradePrice = trade.getTradePrice().getValue();
        this.state = trade.getState();
        this.disputeState = trade.getDisputeState();
        this.tradePeriodState = trade.getTradePeriodState();
        this.arbitratorBtcPubKey = trade.getArbitratorBtcPubKey();
        this.contractHash = trade.getContractHash();
        NodeAddress mediatorNodeAddress = trade.getMediatorNodeAddress();
        if (mediatorNodeAddress != null)
            this.mediatorNodeAddress = mediatorNodeAddress.getFullAddress();
        this.takerContractSignature = trade.getTakerContractSignature();
        this.makerContractSignature = trade.getMakerContractSignature();
        NodeAddress arbitratorNodeAddress = trade.getArbitratorNodeAddress();
        if (arbitratorNodeAddress != null)
            this.arbitratorNodeAddress = arbitratorNodeAddress.getFullAddress();
        NodeAddress tradingPeerNodeAddress = trade.getTradingPeerNodeAddress();
        if (tradingPeerNodeAddress != null)
            this.tradingPeerNodeAddress = tradingPeerNodeAddress.getFullAddress();
        this.takerPaymentAccountId = trade.getTakerPaymentAccountId();
        this.errorMessage = trade.getErrorMessage();
        this.counterCurrencyTxId = trade.getCounterCurrencyTxId();
    }

}
