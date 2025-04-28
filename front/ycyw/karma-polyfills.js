;(function () {
  if (typeof global === 'undefined') {
    window.global = window
  }

  if (typeof process === 'undefined') {
    window.process = {
      env: { DEBUG: undefined },
      nextTick: function (callback) {
        setTimeout(callback, 0)
      }
    }
  }

  if (typeof Buffer === 'undefined') {
    window.Buffer = []
  }
})()
