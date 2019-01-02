package bisq.httpapi.model.payment;

import bisq.core.payment.CashAppAccount;
import bisq.core.payment.payload.CashAppAccountPayload;

@SuppressWarnings("deprecation")
public class CashAppPaymentAccountConverter extends AbstractPaymentAccountConverter<CashAppAccount, CashAppAccountPayload, CashAppPaymentAccount> {

    @Override
    public CashAppAccount toBusinessModel(CashAppPaymentAccount rest) {
        CashAppAccount business = new CashAppAccount();
        business.init();
        business.setCashTag(rest.cashTag);
        toBusinessModel(business, rest);
        return business;
    }

    @Override
    public CashAppPaymentAccount toRestModel(CashAppAccount business) {
        CashAppPaymentAccount rest = toRestModel((CashAppAccountPayload) business.getPaymentAccountPayload());
        toRestModel(rest, business);
        return rest;
    }

    @Override
    public CashAppPaymentAccount toRestModel(CashAppAccountPayload business) {
        CashAppPaymentAccount rest = new CashAppPaymentAccount();
        rest.cashTag = business.getCashTag();
        toRestModel(rest, business);
        return rest;

    }

}