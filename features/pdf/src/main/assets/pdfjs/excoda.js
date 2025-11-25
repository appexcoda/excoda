(function () {
  console.log("[ExcodaPdf] Bridge initializing");

  function injectHeightFix() {
    const style = document.createElement("style");
    style.id = "excoda-height-fix";
    style.textContent = `
      html {
        height: 100% !important;
        min-height: 100% !important;
      }
      body {
        margin: 0 !important;
        padding: 0 !important;
        width: 100% !important;
        height: 100% !important;
        min-height: 100% !important;
        position: fixed !important;
        top: 0 !important;
        left: 0 !important;
        right: 0 !important;
        bottom: 0 !important;
      }
      #outerContainer,
      #mainContainer,
      #viewerContainer {
        position: absolute !important;
        top: 0 !important;
        left: 0 !important;
        right: 0 !important;
        bottom: 0 !important;
        width: 100% !important;
        height: 100% !important;
      }
    `;
    const existing = document.getElementById("excoda-height-fix");
    if (existing) {
      existing.remove();
    }
    document.head.appendChild(style);

    setTimeout(function () {
      document.body.style.display = "none";
      document.body.offsetHeight;
      document.body.style.display = "";
    }, 100);
  }

  function setupListeners() {
    const app = window.PDFViewerApplication;
    if (!app || !app.eventBus) {
      console.log("[ExcodaPdf] App or eventBus not ready");
      return false;
    }
    console.log("[ExcodaPdf] EventBus available, setting up listeners");

    app.eventBus.on("documentloaded", function () {
      console.log("[ExcodaPdf] documentloaded event");
    });

    app.eventBus.on("pagesinit", function () {
      console.log("[ExcodaPdf] pagesinit event");
      injectHeightFix();
    });

    app.eventBus.on("pagerendered", function (evt) {
      console.log("[ExcodaPdf] pagerendered:", evt.pageNumber);
    });

    return true;
  }

  function overrideSaveButton() {
    setTimeout(function() {
      var saveButton = document.getElementById('downloadButton');
      if (saveButton) {
        console.log('[ExcodaPdf] Overriding save button');

        // Remove existing listeners by cloning
        var newButton = saveButton.cloneNode(true);
        saveButton.parentNode.replaceChild(newButton, saveButton);

        // Add our custom handler
        newButton.addEventListener('click', function(e) {
          e.preventDefault();
          e.stopPropagation();

          console.log('[ExcodaPdf] Save button clicked, extracting PDF');
          if (typeof PDFViewerApplication === 'undefined' || !PDFViewerApplication.pdfDocument) {
            console.error('[ExcodaPdf] PDFViewerApplication not available');
            return;
          }

          PDFViewerApplication.pdfDocument.saveDocument().then(function(data) {
            var binary = '';
            var bytes = new Uint8Array(data);
            var len = bytes.byteLength;
            for (var i = 0; i < len; i++) {
              binary += String.fromCharCode(bytes[i]);
            }
            var base64 = 'data:application/pdf;base64,' + btoa(binary);

            if (typeof AndroidDownload !== 'undefined' && AndroidDownload.saveFile) {
              AndroidDownload.saveFile(base64);
              console.log('[ExcodaPdf] Save data sent to Android');
            } else {
              console.error('[ExcodaPdf] AndroidDownload interface not available');
            }
          }).catch(function(error) {
            console.error('[ExcodaPdf] Save failed:', error);
          });
        });
      } else {
        console.warn('[ExcodaPdf] Save button not found');
      }
    }, 1000);
  }

  function waitForApp() {
    if (setupListeners()) {
      console.log("[ExcodaPdf] Successfully initialized");
      return;
    }
    const app = window.PDFViewerApplication;
    if (app && app.initializedPromise) {
      console.log("[ExcodaPdf] Waiting for initializedPromise");
      app.initializedPromise.then(function () {
        console.log("[ExcodaPdf] initializedPromise resolved");
        setupListeners();
      }).catch(function (error) {
        console.error("[ExcodaPdf] initializedPromise rejected:", error);
      });
    } else {
      setTimeout(waitForApp, 100);
    }
  }

  // Expose API for document switching
  window.ExcodaPdf = {
    openDocument: function (url) {
      console.log("[ExcodaPdf] openDocument called with:", url);

      if (!window.PDFViewerApplication) {
        return Promise.reject(new Error("PDFViewerApplication not available"));
      }

      if (!window.PDFViewerApplication.initialized) {
        return Promise.reject(new Error("PDFViewerApplication not initialized"));
      }

      // Close current document first
      return window.PDFViewerApplication.close().then(function () {
        console.log("[ExcodaPdf] Previous document closed");

        // Open new document
        return window.PDFViewerApplication.open({
          url: url,
          originalUrl: url
        });
      }).then(function () {
        console.log("[ExcodaPdf] New document opened successfully");

        // Re-inject height fix after document switch
        setTimeout(function () {
          injectHeightFix();
        }, 100);
      }).catch(function (error) {
        console.error("[ExcodaPdf] Failed to switch document:", error);
        throw error;
      });
    }
  };

  // Initial setup
  injectHeightFix();

  document.addEventListener("webviewerloaded", function () {
    console.log("[ExcodaPdf] webviewerloaded event");
    injectHeightFix();
    waitForApp();
    overrideSaveButton();
  }, { once: true });

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", function () {
      console.log("[ExcodaPdf] DOMContentLoaded");
      injectHeightFix();
      setTimeout(waitForApp, 100);
    });
  } else {
    setTimeout(waitForApp, 100);
  }
})();