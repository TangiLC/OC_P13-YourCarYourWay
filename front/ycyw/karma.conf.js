module.exports = function (config) {
  config.set({
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    reporters: ['progress', 'kjhtml', 'coverage-istanbul'],
    coverageIstanbulReporter: {
      dir: require('path').join(__dirname, './coverage'),
      reports: ['html', 'lcovonly', 'text-summary'],
      fixWebpackSourcePaths: true
    },

    files: [
      {
        pattern: './karma-polyfills.js',
        watched: false,
        included: true,
        served: true
      }
    ],

    beforeMiddleware: ['webpackBlocker']
  })
}
