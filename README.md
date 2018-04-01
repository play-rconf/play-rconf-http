# Play Remote Configuration - HTTP


[![Latest release](https://img.shields.io/badge/latest_release-18.04-orange.svg)](https://github.com/play-rconf/play-rconf-http/releases)
[![JitPack](https://jitpack.io/v/play-rconf/play-rconf-http.svg)](https://jitpack.io/#play-rconf/play-rconf-http)
[![Build](https://img.shields.io/travis-ci/play-rconf/play-rconf-http.svg?branch=master&style=flat)](https://travis-ci.org/play-rconf/play-rconf-http)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/play-rconf/play-rconf-http/master/LICENSE)

Retrieves configuration hosted behind a simple HTTP server
*****

## About this project
In production, it is not always easy to manage the configuration files of a
Play Framework application, especially when it running on multiple servers.
The purpose of this project is to provide a simple way to use a remote
configuration with a Play Framework application.



## How to use

To enable this provider, just add the classpath `"io.playrconf.provider.HttpProvider"`
and the following configuration:

```hocon
remote-configuration {

  ## Provider - HTTP
  # ~~~~~
  # Retrieves configuration from a simple HTTP server
  http {

    # URL where is located the configuration file to fetch. You can
    # use basic authentication
    url = "https://username:password@domain.com/my-configuration.conf"
    url = ${?REMOTECONF_HTTP_URL}
  }
}
```


## License
This project is released under terms of the [MIT license](https://raw.githubusercontent.com/play-rconf/play-rconf-http/master/LICENSE).