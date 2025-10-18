const TOAST_CONFIG = {
  autohide: true,
  delay: 5000,
};

function showToast(type, message, delay = 5000) {
  const toastElement = document.getElementById(type + "Toast");
  const messageElement = document.getElementById(type + "Message");

  if (toastElement && messageElement) {
    messageElement.textContent = message;
    const toast = new bootstrap.Toast(toastElement, {
      autohide: true,
      delay: delay,
    });
    toast.show();
  } else {
    console.warn(`Toast elements not found for type: ${type}`);
  }
}

function showSuccessToast(message) {
  showToast("success", message);
}

function showErrorToast(message) {
  showToast("error", message);
}

function showWarningToast(message) {
  showToast("warning", message);
}

function showInfoToast(message) {
  showToast("info", message);
}

// Initialize toast system for server-side messages
function initializeToastMessages() {
  // This function will be called by pages that need to show server-side messages
  // The actual message handling is done in the toast-scripts fragment
}

// Auto-show toasts from URL parameters (for redirects)
function checkUrlToastParams() {
  const urlParams = new URLSearchParams(window.location.search);

  if (urlParams.get("success")) {
    showSuccessToast(decodeURIComponent(urlParams.get("success")));
    // Clean URL
    urlParams.delete("success");
    window.history.replaceState(
      {},
      document.title,
      window.location.pathname + (urlParams.toString() ? "?" + urlParams.toString() : "")
    );
  }

  if (urlParams.get("error")) {
    showErrorToast(decodeURIComponent(urlParams.get("error")));
    // Clean URL
    urlParams.delete("error");
    window.history.replaceState(
      {},
      document.title,
      window.location.pathname + (urlParams.toString() ? "?" + urlParams.toString() : "")
    );
  }

  if (urlParams.get("warning")) {
    showWarningToast(decodeURIComponent(urlParams.get("warning")));
    // Clean URL
    urlParams.delete("warning");
    window.history.replaceState(
      {},
      document.title,
      window.location.pathname + (urlParams.toString() ? "?" + urlParams.toString() : "")
    );
  }

  if (urlParams.get("info")) {
    showInfoToast(decodeURIComponent(urlParams.get("info")));
    // Clean URL
    urlParams.delete("info");
    window.history.replaceState(
      {},
      document.title,
      window.location.pathname + (urlParams.toString() ? "?" + urlParams.toString() : "")
    );
  }
}

// Auto-initialize when DOM is loaded
document.addEventListener("DOMContentLoaded", function () {
  checkUrlToastParams();
});
