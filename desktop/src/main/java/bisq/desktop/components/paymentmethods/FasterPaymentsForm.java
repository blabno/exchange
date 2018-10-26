/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.components.paymentmethods;

import bisq.desktop.components.InputTextField;
import bisq.desktop.util.FormBuilder;
import bisq.desktop.util.Layout;
import bisq.desktop.util.validation.AccountNrValidator;
import bisq.desktop.util.validation.BranchIdValidator;

import bisq.core.locale.Res;
import bisq.core.locale.TradeCurrency;
import bisq.core.payment.AccountAgeWitnessService;
import bisq.core.payment.FasterPaymentsAccount;
import bisq.core.payment.PaymentAccount;
import bisq.core.payment.payload.FasterPaymentsAccountPayload;
import bisq.core.payment.payload.PaymentAccountPayload;
import bisq.core.util.BSFormatter;
import bisq.core.util.validation.InputValidator;

import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import static bisq.desktop.util.FormBuilder.addTopLabelTextField;

public class FasterPaymentsForm extends PaymentMethodForm {
    private static final String UK_SORT_CODE = "UK sort code";

    public static int addFormForBuyer(GridPane gridPane, int gridRow,
                                      PaymentAccountPayload paymentAccountPayload) {
        // do not translate as it is used in english only
        FormBuilder.addTopLabelTextField(gridPane, ++gridRow, UK_SORT_CODE,
                ((FasterPaymentsAccountPayload) paymentAccountPayload).getSortCode());
        FormBuilder.addTopLabelTextField(gridPane, ++gridRow, Res.get("payment.accountNr"),
                ((FasterPaymentsAccountPayload) paymentAccountPayload).getAccountNr());
        return gridRow;
    }


    private final FasterPaymentsAccount fasterPaymentsAccount;
    private InputTextField accountNrInputTextField;
    private InputTextField sortCodeInputTextField;

    public FasterPaymentsForm(PaymentAccount paymentAccount, AccountAgeWitnessService accountAgeWitnessService, InputValidator inputValidator, GridPane gridPane,
                              int gridRow, BSFormatter formatter) {
        super(paymentAccount, accountAgeWitnessService, inputValidator, gridPane, gridRow, formatter);
        this.fasterPaymentsAccount = (FasterPaymentsAccount) paymentAccount;
    }

    @Override
    public void addFormForAddAccount() {
        gridRowFrom = gridRow + 1;
        // do not translate as it is used in english only
        sortCodeInputTextField = FormBuilder.addInputTextField(gridPane, ++gridRow, UK_SORT_CODE);
        sortCodeInputTextField.setValidator(inputValidator);
        sortCodeInputTextField.setValidator(new BranchIdValidator("GB"));
        sortCodeInputTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            fasterPaymentsAccount.setSortCode(newValue);
            updateFromInputs();
        });

        accountNrInputTextField = FormBuilder.addInputTextField(gridPane, ++gridRow, Res.get("payment.accountNr"));
        accountNrInputTextField.setValidator(new AccountNrValidator("GB"));
        accountNrInputTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            fasterPaymentsAccount.setAccountNr(newValue);
            updateFromInputs();
        });

        TradeCurrency singleTradeCurrency = fasterPaymentsAccount.getSingleTradeCurrency();
        String nameAndCode = singleTradeCurrency != null ? singleTradeCurrency.getNameAndCode() : "";
        FormBuilder.addTopLabelTextField(gridPane, ++gridRow, Res.get("shared.currency"),
                nameAndCode);
        addLimitations();
        addAccountNameTextFieldWithAutoFillToggleButton();
    }

    @Override
    protected void autoFillNameTextField() {
        setAccountNameWithString(accountNrInputTextField.getText());
    }

    @Override
    public void addFormForDisplayAccount() {
        gridRowFrom = gridRow;
        addTopLabelTextField(gridPane, gridRow, Res.get("payment.account.name"),
                fasterPaymentsAccount.getAccountName(), Layout.FIRST_ROW_AND_GROUP_DISTANCE);
        FormBuilder.addTopLabelTextField(gridPane, ++gridRow, Res.get("shared.paymentMethod"),
                Res.get(fasterPaymentsAccount.getPaymentMethod().getId()));
        // do not translate as it is used in english only
        FormBuilder.addTopLabelTextField(gridPane, ++gridRow, UK_SORT_CODE, fasterPaymentsAccount.getSortCode());
        TextField field = FormBuilder.addTopLabelTextField(gridPane, ++gridRow, Res.get("payment.accountNr"),
                fasterPaymentsAccount.getAccountNr()).second;
        field.setMouseTransparent(false);
        TradeCurrency singleTradeCurrency = fasterPaymentsAccount.getSingleTradeCurrency();
        String nameAndCode = singleTradeCurrency != null ? singleTradeCurrency.getNameAndCode() : "";
        FormBuilder.addTopLabelTextField(gridPane, ++gridRow, Res.get("shared.currency"), nameAndCode);
        addLimitations();
    }

    @Override
    public void updateAllInputsValid() {
        allInputsValid.set(isAccountNameValid()
                && sortCodeInputTextField.getValidator().validate(fasterPaymentsAccount.getSortCode()).isValid
                && accountNrInputTextField.getValidator().validate(fasterPaymentsAccount.getAccountNr()).isValid
                && fasterPaymentsAccount.getTradeCurrencies().size() > 0);
    }
}
