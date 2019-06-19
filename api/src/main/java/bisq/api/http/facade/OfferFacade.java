package bisq.api.http.facade;

import bisq.api.http.exceptions.NotBootstrappedException;
import bisq.api.http.exceptions.NotFoundException;
import bisq.api.http.model.CreateOfferRequest;
import bisq.api.http.model.OfferDetail;
import bisq.api.http.model.PriceType;
import bisq.api.http.service.endpoint.OfferBuilder;

import bisq.core.btc.wallet.Restrictions;
import bisq.core.offer.Offer;
import bisq.core.offer.OfferBookService;
import bisq.core.offer.OfferPayload;
import bisq.core.offer.OfferUtil;
import bisq.core.offer.OpenOffer;
import bisq.core.offer.OpenOfferManager;
import bisq.core.trade.Trade;
import bisq.core.trade.TradeManager;
import bisq.core.user.Preferences;

import bisq.network.p2p.P2PService;

import bisq.common.UserThread;

import org.bitcoinj.core.Coin;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import static java.util.stream.Collectors.toList;



import javax.validation.ValidationException;

public class OfferFacade {

    private final OfferBookService offerBookService;
    private final TradeManager tradeManager;
    private final OpenOfferManager openOfferManager;
    private final OfferBuilder offerBuilder;
    private final P2PService p2PService;
    private final Preferences preferences;

    @Inject
    public OfferFacade(OfferBookService offerBookService,
                       TradeManager tradeManager,
                       OpenOfferManager openOfferManager,
                       OfferBuilder offerBuilder,
                       P2PService p2PService,
                       Preferences preferences) {
        this.offerBookService = offerBookService;
        this.tradeManager = tradeManager;
        this.openOfferManager = openOfferManager;
        this.offerBuilder = offerBuilder;
        this.p2PService = p2PService;
        this.preferences = preferences;
    }

    public List<OfferDetail> getAllOffers() {
        return offerBookService.getOffers().stream().map(OfferDetail::new).collect(toList());
    }

    public Offer findOffer(String offerId) {
        Optional<Offer> offerOptional = offerBookService.getOffers().stream()
                .filter(offer -> offer.getId().equals(offerId))
                .findAny();
        if (!offerOptional.isPresent()) {
            throw new NotFoundException("Offer not found: " + offerId);
        }
        return offerOptional.get();
    }

    public CompletableFuture<Void> cancelOffer(String offerId) {
        CompletableFuture<Void> futureResult = new CompletableFuture<>();
        UserThread.execute(() -> {
            if (!p2PService.isBootstrapped()) {
                futureResult.completeExceptionally(new NotBootstrappedException());
                return;
            }

            Optional<OpenOffer> openOfferById = openOfferManager.getOpenOfferById(offerId);
            if (!openOfferById.isPresent()) {
                futureResult.completeExceptionally(new NotFoundException("Offer not found: " + offerId));
                return;
            }

            openOfferManager.removeOpenOffer(openOfferById.get(),
                    () -> futureResult.complete(null),
                    errorMessage -> futureResult.completeExceptionally(new RuntimeException(errorMessage)));
        });
        return futureResult;
    }

    public CompletableFuture<Offer> createOffer(CreateOfferRequest input) {
        OfferPayload.Direction direction = OfferPayload.Direction.valueOf(input.direction);
        PriceType priceType = PriceType.valueOf(input.priceType);
        Double marketPriceMargin = input.percentageFromMarketPrice == null ? null : input.percentageFromMarketPrice.doubleValue();
        String accountId = input.accountId;
        long amount = input.amount;
        long minAmount = input.minAmount;
        boolean useMarketBasedPrice = PriceType.PERCENTAGE.equals(priceType);
        String marketPair = input.marketPair;
        long fiatPrice = input.fixedPrice;
        Long buyerSecurityDeposit = input.buyerSecurityDeposit;

        Offer offer;
        try {
            offer = offerBuilder.build(accountId, direction, amount, minAmount, useMarketBasedPrice,
                    marketPriceMargin, marketPair, fiatPrice, buyerSecurityDeposit);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        boolean isBuyOffer = OfferUtil.isBuyOffer(direction);
        Coin reservedFundsForOffer = isBuyOffer ? Coin.valueOf(preferences.getBuyerSecurityDepositAsLong()) : Restrictions.getMinSellerSecurityDepositAsCoin();
        if (!isBuyOffer)
            reservedFundsForOffer = reservedFundsForOffer.add(Coin.valueOf(amount));

        CompletableFuture<Offer> futureResult = new CompletableFuture<>();
//        TODO check if there is sufficient money cause openOfferManager will log exception and pass just message

        // @bernard: ValidateOffer returns plenty of diff. error messages. To handle all separately would be a big
        // overkill. I think it should be ok to just display the errorMessage and not handle the diff. errors on your
        // side.
        // TODO check for tradeLimit is missing in ValidateOffer
        openOfferManager.placeOffer(offer,
                reservedFundsForOffer,
                true,
                transaction -> futureResult.complete(offer),
                errorMessage -> {
                    final Matcher matcher = Pattern.compile(".*Error message: (.*)").matcher(errorMessage);
                    final String nestedMessage = matcher.find() ? matcher.group(1) : errorMessage;
                    if (errorMessage.contains("Insufficient money"))
                        futureResult.completeExceptionally(new ValidationException(nestedMessage));
                    else if (errorMessage.contains("Amount is larger"))
                        futureResult.completeExceptionally(new ValidationException(nestedMessage));
                    else
                        futureResult.completeExceptionally(new RuntimeException(errorMessage));
                });

        return futureResult;
    }

    public CompletableFuture<Trade> offerTake(String offerId, String paymentAccountId, long amount, boolean useSavingsWallet, @Nullable Long maxFundsForTrade) {
        Offer offer;
        try {
            offer = findOffer(offerId);
        } catch (NotFoundException e) {
            return CompletableFuture.failedFuture(e);
        }

        return tradeManager.onTakeOffer(Coin.valueOf(amount),
                offer.getPrice().getValue(),
                offer,
                paymentAccountId,
                useSavingsWallet,
                maxFundsForTrade);
    }
}
