package bisq.httpapi.service.endpoint;

import bisq.core.btc.Balances;
import bisq.core.btc.exceptions.AddressEntryException;
import bisq.core.btc.exceptions.InsufficientFundsException;

import bisq.common.UserThread;

import org.bitcoinj.core.Coin;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;



import bisq.httpapi.exceptions.AmountTooLowException;
import bisq.httpapi.facade.WalletFacade;
import bisq.httpapi.model.AuthForm;
import bisq.httpapi.model.SeedWords;
import bisq.httpapi.model.SeedWordsRestore;
import bisq.httpapi.model.WalletAddress;
import bisq.httpapi.model.WalletAddressList;
import bisq.httpapi.model.WalletTransactionList;
import bisq.httpapi.model.WithdrawFundsForm;
import bisq.httpapi.service.ExperimentalFeature;
import io.dropwizard.jersey.validation.ValidationErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "wallet", authorizations = @Authorization(value = "accessToken"))
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class WalletEndpoint {

    private final Balances balances;
    private final ExperimentalFeature experimentalFeature;
    private final WalletFacade walletFacade;

    @Inject
    public WalletEndpoint(Balances balances, ExperimentalFeature experimentalFeature, WalletFacade walletFacade) {
        this.balances = balances;
        this.experimentalFeature = experimentalFeature;
        this.walletFacade = walletFacade;
    }

    @ApiOperation(value = "Get wallet details", response = bisq.httpapi.model.Balances.class, notes = ExperimentalFeature.NOTE)
    @GET
    public void getWalletDetails(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                long availableBalance = this.balances.getAvailableBalance().get().value;
                long reservedBalance = this.balances.getReservedBalance().get().value;
                long lockedBalance = this.balances.getLockedBalance().get().value;
                bisq.httpapi.model.Balances balances = new bisq.httpapi.model.Balances(availableBalance,
                        reservedBalance,
                        lockedBalance);
                asyncResponse.resume(balances);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Get wallet addresses", response = WalletAddressList.class, notes = ExperimentalFeature.NOTE)
    @GET
    @Path("/addresses")
    public void getAddresses(@Suspended AsyncResponse asyncResponse, @QueryParam("purpose") WalletFacade.WalletAddressPurpose purpose) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                WalletAddressList walletAddresses = walletFacade.getWalletAddresses(purpose);
                asyncResponse.resume(walletAddresses);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Get or create wallet address", response = WalletAddress.class, notes = ExperimentalFeature.NOTE)
    @POST
    @Path("/addresses") //TODO should path be "addresses" ?
    public void getOrCreateAvailableUnusedWalletAddresses(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                WalletAddress addresses = walletFacade.getOrCreateAvailableUnusedWalletAddresses();
                asyncResponse.resume(addresses);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Get wallet seed words", response = SeedWords.class, notes = ExperimentalFeature.NOTE)
    @POST
    @Path("/seed-words/retrieve")
    public void getSeedWords(@Suspended AsyncResponse asyncResponse, AuthForm form) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                String password = null == form ? null : form.password;
                SeedWords seedWords = walletFacade.getSeedWords(password);
                asyncResponse.resume(seedWords);
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Restore wallet from seed words", notes = ExperimentalFeature.NOTE)
    @POST
    @Path("/seed-words/restore")
    public void restoreWalletFromSeedWords(@Suspended AsyncResponse asyncResponse, @Valid @NotNull SeedWordsRestore data) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                walletFacade.restoreWalletFromSeedWords(data.mnemonicCode, data.walletCreationDate, data.password)
                        .thenApply(response -> asyncResponse.resume(Response.noContent().build()))
                        .exceptionally(e -> {
                            Throwable cause = e.getCause();
                            Response.ResponseBuilder responseBuilder;

                            String message = cause.getMessage();
                            responseBuilder = Response.status(500);
                            if (null != message)
                                responseBuilder.entity(new ValidationErrorMessage(ImmutableList.of(message)));
                            log.error("Unable to restore wallet from seed", cause);
                            return asyncResponse.resume(responseBuilder.build());
                        });
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Get wallet transactions", response = WalletTransactionList.class, notes = ExperimentalFeature.NOTE)
    @GET
    @Path("/transactions")
    public void getTransactions(@Suspended AsyncResponse asyncResponse) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                asyncResponse.resume(walletFacade.getWalletTransactions());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }

    @ApiOperation(value = "Withdraw funds", notes = ExperimentalFeature.NOTE)
    @POST
    @Path("/withdraw")
    public void withdrawFunds(@Suspended AsyncResponse asyncResponse, @Valid WithdrawFundsForm data) {
        UserThread.execute(() -> {
            try {
                experimentalFeature.assertEnabled();
                HashSet<String> sourceAddresses = new HashSet<>(data.sourceAddresses);
                Coin amountAsCoin = Coin.valueOf(data.amount);
                boolean feeExcluded = data.feeExcluded;
                String targetAddress = data.targetAddress;
                try {
                    walletFacade.withdrawFunds(sourceAddresses, amountAsCoin, feeExcluded, targetAddress);
                    asyncResponse.resume(Response.noContent().build());
                } catch (AddressEntryException e) {
                    throw new ValidationException(e.getMessage());
                } catch (InsufficientFundsException e) {
                    throw new WebApplicationException(e.getMessage(), 423);
                } catch (AmountTooLowException e) {
                    throw new WebApplicationException(e.getMessage(), 424);
                }
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}
