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
import io.playrconf.sdk.AbstractProvider;
import io.playrconf.sdk.FileCfgObject;
import io.playrconf.sdk.KeyValueCfgObject;
import io.playrconf.sdk.exception.RemoteConfException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Retrieves configuration hosted behind a simple HTTP server.
 *
 * @author Thibault Meyer
 * @since 18.03.31
 */
public class HttpProvider extends AbstractProvider {

    /**
     * Contains the provider version.
     */
    private static String providerVersion;

    @Override
    public String getName() {
        return "HTTP";
    }

    @Override
    public String getVersion() {
        if (HttpProvider.providerVersion == null) {
            synchronized (HttpProvider.class) {
                final Properties properties = new Properties();
                final InputStream is = HttpProvider.class.getClassLoader()
                    .getResourceAsStream("playrconf-http.properties");
                try {
                    properties.load(is);
                    HttpProvider.providerVersion = properties.getProperty("playrconf.http.version", "unknown");
                    properties.clear();
                    is.close();
                } catch (final IOException ignore) {
                }
            }
        }
        return HttpProvider.providerVersion;
    }

    @Override
    public String getConfigurationObjectName() {
        return "http";
    }

    @Override
    public void loadData(final Config config,
                         final Consumer<KeyValueCfgObject> kvObjConsumer,
                         final Consumer<FileCfgObject> fileObjConsumer) throws ConfigException, RemoteConfException {
        try {
            final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

            if (config.hasPath("basic-auth.username") && config.hasPath("basic-auth.password")) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                    config.getString("basic-auth.username"),
                    config.getString("basic-auth.password")
                );
                credentialsProvider.setCredentials(AuthScope.ANY, credentials);
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            final HttpClient httpClient = httpClientBuilder
                .setUserAgent("Play-RemoteConfiguration-Client")
                .build();

            final HttpResponse httpResponse = httpClient.execute(new HttpGet(config.getString("url")));
            final String configurationContent = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");

            final Config remoteConfiguration = ConfigFactory.parseString(configurationContent);

            remoteConfiguration.entrySet().forEach(entry -> {
                final String value = entry.getValue().render();
                if (isFile(value)) {
                    fileObjConsumer.accept(
                        new FileCfgObject(entry.getKey(), value)
                    );
                } else {
                    kvObjConsumer.accept(
                        new KeyValueCfgObject(entry.getKey(), value)
                    );
                }
            });
        } catch (final ConfigException ex2) {
            if (ex2.getCause() != null) {
                throw new ConfigException.BadPath(
                    config.getString("url"),
                    ex2.getCause().getClass().getName(),
                    ex2.getCause()
                );
            } else {
                throw new ConfigException.ValidationFailed(
                    Collections.singletonList(
                        new ConfigException.ValidationProblem(
                            config.getString("url"),
                            ex2.origin(),
                            ex2.getMessage()
                        )
                    )
                );
            }
        } catch (final MalformedURLException | UnknownHostException ex) {
            throw new ConfigException.BadValue("url", ex.getMessage());
        } catch (final IOException ex) {
            throw new ConfigException.IO(config.origin(), ex.getMessage());
        }
    }
}
