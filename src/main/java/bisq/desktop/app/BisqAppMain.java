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

package bisq.desktop.app;

import bisq.core.app.AppOptionKeys;
import bisq.core.app.BisqEnvironment;
import bisq.core.app.BisqExecutable;
import bisq.core.btc.BaseCurrencyNetwork;
import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.Res;

import bisq.common.app.Capabilities;
import bisq.common.util.Utilities;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import java.util.ArrayList;
import java.util.Arrays;

import static bisq.core.app.BisqEnvironment.DEFAULT_APP_NAME;
import static bisq.core.app.BisqEnvironment.DEFAULT_USER_DATA_DIR;

public class BisqAppMain extends BisqExecutable {

    static {
        Utilities.removeCryptographyRestrictions();
    }

    public static void main(String[] args) throws Exception {
        // We don't want to do the full argument parsing here as that might easily change in update versions
        // So we only handle the absolute minimum which is APP_NAME, APP_DATA_DIR_KEY and USER_DATA_DIR
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        parser.accepts(AppOptionKeys.USER_DATA_DIR_KEY, description("User data directory", DEFAULT_USER_DATA_DIR))
                .withRequiredArg();
        parser.accepts(AppOptionKeys.APP_NAME_KEY, description("Application name", DEFAULT_APP_NAME))
                .withRequiredArg();

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException ex) {
            System.out.println("error: " + ex.getMessage());
            System.out.println();
            parser.printHelpOn(System.out);
            System.exit(EXIT_FAILURE);
            return;
        }
        BisqEnvironment bisqEnvironment = getBisqEnvironment(options);

        // need to call that before bisqAppMain().execute(args)
        initAppDir(bisqEnvironment.getProperty(AppOptionKeys.APP_DATA_DIR_KEY));

        // For some reason the JavaFX launch process results in us losing the thread context class loader: reset it.
        // In order to work around a bug in JavaFX 8u25 and below, you must include the following code as the first line of your realMain method:
        Thread.currentThread().setContextClassLoader(BisqAppMain.class.getClassLoader());

        new BisqAppMain().execute(args);
    }

    @Override
    protected void doExecute(OptionSet options) {
        if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))
            Security.addProvider(new BouncyCastleProvider());
        final BaseCurrencyNetwork baseCurrencyNetwork = BisqEnvironment.getBaseCurrencyNetwork();
        final String currencyCode = baseCurrencyNetwork.getCurrencyCode();
        Res.setBaseCurrencyCode(currencyCode);
        Res.setBaseCurrencyName(baseCurrencyNetwork.getCurrencyName());
        CurrencyUtil.setBaseCurrencyCode(currencyCode);

        Capabilities.setSupportedCapabilities(new ArrayList<>(Arrays.asList(
                Capabilities.Capability.TRADE_STATISTICS.ordinal(),
                Capabilities.Capability.TRADE_STATISTICS_2.ordinal(),
                Capabilities.Capability.ACCOUNT_AGE_WITNESS.ordinal(),
                Capabilities.Capability.COMP_REQUEST.ordinal(),
                Capabilities.Capability.VOTE.ordinal()
        )));

        final BisqEnvironment bisqEnvironment = getBisqEnvironment(options);
        BisqApp.setEnvironment(bisqEnvironment);
        final Injector injector = Guice.createInjector(new BisqAppModule(bisqEnvironment));
        BisqApp.setInjector(injector);
//        REFACTOR just testing pure core mode; restore that javafx launch once wallet init is refactored
//        javafx.application.Application.launch(BisqApp.class);
        injector.getInstance(ClientAppSetup.class).start();
    }
}
