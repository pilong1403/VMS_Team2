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

function initializeToastFromFlashAttributes() {
  // This function expects to be called in a Thymeleaf context
  // where success and error variables are available
  // Success message handling will be done in each template
  // since Thymeleaf inline JavaScript is template-specific
}

function getToastContainerHTML() {
  return `
    <!-- Toast Container -->
    <div class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 1200;">
      <!-- Success Toast -->
      <div id="successToast" class="toast align-items-center text-bg-success border-0" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="d-flex">
          <div class="toast-body">
            <i class="bi bi-check-circle me-2"></i>
            <span id="successMessage">Success message</span>
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
      </div>
      
      <!-- Error Toast -->
      <div id="errorToast" class="toast align-items-center text-bg-danger border-0" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="d-flex">
          <div class="toast-body">
            <i class="bi bi-exclamation-triangle me-2"></i>
            <span id="errorMessage">Error message</span>
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
      </div>
      
      <!-- Warning Toast -->
      <div id="warningToast" class="toast align-items-center text-bg-warning border-0" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="d-flex">
          <div class="toast-body">
            <i class="bi bi-exclamation-circle me-2"></i>
            <span id="warningMessage">Warning message</span>
          </div>
          <button type="button" class="btn-close btn-close-dark me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
      </div>
      
      <!-- Info Toast -->
      <div id="infoToast" class="toast align-items-center text-bg-info border-0" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="d-flex">
          <div class="toast-body">
            <i class="bi bi-info-circle me-2"></i>
            <span id="infoMessage">Info message</span>
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
      </div>
    </div>
  `;
}
