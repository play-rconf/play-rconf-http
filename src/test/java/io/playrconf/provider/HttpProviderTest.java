/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 The Play Remote Configuration Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.playrconf.provider;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import io.playrconf.sdk.FileCfgObject;
import io.playrconf.sdk.Provider;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * HttpProviderTest.
 *
 * @author Thibault Meyer
 * @since 18.03.31
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HttpProviderTest {

    /**
     * Initial configuration.
     */
    private static final Config INITIAL_CONFIGURATION = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "http.url = \"https://pastebin.com/raw/x28wW7J8\"\n"
    );

    /**
     * Initial configuration. Requested remote configuration file does not exists.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_NOT_FOUND = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "http.url = \"https://pastebin.com/raw/KaDmFAYn87\"\n"
    );

    /**
     * Initial configuration. Remote server can't be resolved.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_UNKNOWN_HOST = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "http.url = \"https://doma1n-do3s-not-3x15t5-2832893729387.com/config\"\n"
    );

    /**
     * Initial configuration. Requested remote configuration file is not valid.
     */
    private static final Config INITIAL_CONFIGURATION_ERROR_INVALID_FILE = ConfigFactory.parseString(
        "application.hello = \"Bad value\"\n"
            + "http.url = \"https://pastebin.com/raw/KaDmFAYn\"\n"
    );

    @Test
    public void httpTest_001() {
        // Load remote configuration
        final StringBuilder stringBuilder = new StringBuilder(512);
        final Provider provider = new HttpProvider();
        provider.loadData(
            INITIAL_CONFIGURATION.getConfig(provider.getConfigurationObjectName()),
            keyValueCfgObject -> keyValueCfgObject.apply(stringBuilder),
            FileCfgObject::apply
        );
        final Config remoteConfig = ConfigFactory
            .parseString(stringBuilder.toString())
            .withFallback(INITIAL_CONFIGURATION);

        // Test version
        final Properties properties = new Properties();
        final InputStream is = HttpProvider.class.getClassLoader()
            .getResourceAsStream("playrconf-http.properties");
        try {
            properties.load(is);
            Assert.assertEquals(
                provider.getVersion(),
                properties.getProperty("playrconf.http.version", "unknown")
            );
            properties.clear();
            is.close();
        } catch (final IOException ignore) {
        }

        // Standard values
        Assert.assertEquals(5, remoteConfig.getInt("application.five"));
        Assert.assertEquals("world", remoteConfig.getString("application.hello"));
        Assert.assertTrue(remoteConfig.getBoolean("application.is-enabled"));
        Assert.assertEquals(4, remoteConfig.getIntList("application.list").size());

        // File
        final File file = new File("./test");
        try {
            final InputStream initialStream = new FileInputStream(file);
            final byte[] buffer = new byte[128];
            final int nbRead = initialStream.read(buffer);
            buffer[nbRead] = '\0';
            Assert.assertTrue(nbRead > 0);
            Assert.assertEquals(
                "Hello World!",
                new String(buffer, 0, nbRead)
            );
        } catch (final IOException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

    @Test(expected = ConfigException.ValidationFailed.class)
    public void httpTest_002() {
        // Load remote configuration
        final StringBuilder stringBuilder = new StringBuilder(512);
        final Provider provider = new HttpProvider();
        provider.loadData(
            INITIAL_CONFIGURATION_ERROR_NOT_FOUND.getConfig(provider.getConfigurationObjectName()),
            keyValueCfgObject -> keyValueCfgObject.apply(stringBuilder),
            FileCfgObject::apply
        );
        ConfigFactory
            .parseString(stringBuilder.toString())
            .withFallback(INITIAL_CONFIGURATION_ERROR_NOT_FOUND);
    }

    @Test(expected = ConfigException.BadValue.class)
    public void httpTest_003() {
        // Load remote configuration
        final StringBuilder stringBuilder = new StringBuilder(512);
        final Provider provider = new HttpProvider();
        provider.loadData(
            INITIAL_CONFIGURATION_ERROR_UNKNOWN_HOST.getConfig(provider.getConfigurationObjectName()),
            keyValueCfgObject -> keyValueCfgObject.apply(stringBuilder),
            FileCfgObject::apply
        );
        ConfigFactory
            .parseString(stringBuilder.toString())
            .withFallback(INITIAL_CONFIGURATION_ERROR_UNKNOWN_HOST);
    }

    @Test(expected = ConfigException.ValidationFailed.class)
    public void httpTest_004() {
        // Load remote configuration
        final StringBuilder stringBuilder = new StringBuilder(512);
        final Provider provider = new HttpProvider();
        provider.loadData(
            INITIAL_CONFIGURATION_ERROR_INVALID_FILE.getConfig(provider.getConfigurationObjectName()),
            keyValueCfgObject -> keyValueCfgObject.apply(stringBuilder),
            FileCfgObject::apply
        );
        ConfigFactory
            .parseString(stringBuilder.toString())
            .withFallback(INITIAL_CONFIGURATION_ERROR_INVALID_FILE);
    }
}
