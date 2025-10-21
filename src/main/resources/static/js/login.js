document.addEventListener("DOMContentLoaded", function () {
  const togglePasswordBtn = document.getElementById("togglePassword");
  const passwordInput = document.getElementById("password");
  const toggleIcon = document.getElementById("toggleIcon");
  const form = document.querySelector(".login-form");

  // Toggle password visibility
  if (togglePasswordBtn) {
    togglePasswordBtn.addEventListener("click", function () {
      if (passwordInput.type === "password") {
        passwordInput.type = "text";
        toggleIcon.className = "bi bi-eye-slash";
      } else {
        passwordInput.type = "password";
        toggleIcon.className = "bi bi-eye";
      }
    });
  }

  // Show loading state on form submit
  if (form) {
    form.addEventListener("submit", function () {
      const submitBtn = document.querySelector(".btn-login");
      submitBtn.innerHTML = '<i class="bi bi-hourglass-split me-2"></i>Đang đăng nhập...';
      submitBtn.disabled = true;
    });
  }
});

  document.addEventListener("DOMContentLoaded", function () {
  const gBtn = document.getElementById("btnGoogle");
  if (!gBtn) return;
  gBtn.addEventListener("click", function () {
  gBtn.setAttribute("aria-disabled", "true");
  gBtn.innerHTML = `
        <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        Đang chuyển hướng...
      `;
});
});