// Fixes problems with hot-reload. Thanks to Robert Jaros (https://youtrack.jetbrains.com/issue/KT-32273)
const webpack = require("webpack")
const dotenv = require('dotenv').config({path: '../../../../.env'});

config.resolve.modules.push("../../processedResources/Js/main");
if (!config.devServer && config.output) {
  config.devtool = false
  config.output.filename = "client.js"
}
if (config.devServer) {
  config.devServer.watchOptions = {
    aggregateTimeout: 300,
    poll: 300
  };
  config.devServer.stats = {
    warnings: false
  };
  config.devServer.clientLogLevel = 'error';
}

class KvWebpackPlugin {
  apply(compiler) {
    const fs = require('fs')
    compiler.hooks.watchRun.tapAsync("KvWebpackPlugin", (compiler, callback) => {
      var runCallback = true;
      for (let item of compiler.removedFiles.values()) {
        if (item == config.entry.main) {
          if (!fs.existsSync(item)) {
            fs.watchFile(item, {interval: 50}, (current, previous) => {
              if (current.ino > 0) {
                fs.unwatchFile(item);
                callback();
              }
            });
            runCallback = false;
          }
        }
      }
      if (runCallback) callback();
    });
  }
};
config.plugins.push(new KvWebpackPlugin())

// define env vars dependending on mode

var selfUrl, apiUrl

if (config.mode === "production") { // the build process makes the config object available
  selfUrl = dotenv.parsed.SELF_URL
  apiUrl = dotenv.parsed.API_URL
} else {
  selfUrl = "localhost:8080"
  apiUrl = "localhost:8081"
}

const definePlugin = new webpack.DefinePlugin(
    {
      SELF_URL: selfUrl,
      API_URL: apiUrl
    }
)

config.plugins.push(definePlugin)
