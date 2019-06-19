package bisq.api.http.model.payment;

import bisq.core.payment.HalCashAccount;
import bisq.core.payment.payload.HalCashAccountPayload;

public class HalCashPaymentAccountConverter extends AbstractPaymentAccountConverter<HalCashAccount, HalCashAccountPayload, HalCashPaymentAccount> {

    @Override
    public HalCashAccount toBusinessModel(HalCashPaymentAccount rest) {
        HalCashAccount business = new HalCashAccount();
        business.init();
        business.setMobileNr(rest.mobileNr);
        toBusinessModel(business, rest);
        return business;
    }

    @Override
    public HalCashPaymentAccount toRestModel(HalCashAccount business) {
        HalCashPaymentAccount rest = toRestModel((HalCashAccountPayload) business.getPaymentAccountPayload());
        toRestModel(rest, business);
        return rest;
    }

    @Override
    public HalCashPaymentAccount toRestModel(HalCashAccountPayload business) {
        HalCashPaymentAccount rest = new HalCashPaymentAccount();
        rest.mobileNr = business.getMobileNr();
        toRestModel(rest, business);
        return rest;
    }

}
