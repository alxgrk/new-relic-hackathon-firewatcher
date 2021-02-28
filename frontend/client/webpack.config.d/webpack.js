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

var selfUrl, apiUrl, mode

if (config.mode === "production") { // the build process makes the config object available
  selfUrl = process.env.SELF_URL || dotenv.parsed.SELF_URL
  apiUrl = process.env.API_URL || dotenv.parsed.API_URL
  mode = "PROD"
} else {
  selfUrl = "http://localhost:8080/"
  apiUrl = "http://localhost:8081/"
  mode = "DEV"
}

const definePlugin = new webpack.DefinePlugin(
    {
      SELF_URL: JSON.stringify(selfUrl),
      API_URL: JSON.stringify(apiUrl),
      MODE: JSON.stringify(mode)
    }
)

config.plugins.push(definePlugin)
