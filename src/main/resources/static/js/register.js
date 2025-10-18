document.addEventListener("DOMContentLoaded", function () {
  const togglePasswordBtn = document.getElementById("togglePassword");
  const passwordInput = document.getElementById("password");
  const toggleIcon = document.getElementById("toggleIcon");

  const toggleConfirmPasswordBtn = document.getElementById("toggleConfirmPassword");
  const confirmPasswordInput = document.getElementById("confirmPassword");
  const toggleConfirmIcon = document.getElementById("toggleConfirmIcon");

  const form = document.querySelector(".register-form");

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

  // Toggle confirm password visibility
  if (toggleConfirmPasswordBtn) {
    toggleConfirmPasswordBtn.addEventListener("click", function () {
      if (confirmPasswordInput.type === "password") {
        confirmPasswordInput.type = "text";
        toggleConfirmIcon.className = "bi bi-eye-slash";
      } else {
        confirmPasswordInput.type = "password";
        toggleConfirmIcon.className = "bi bi-eye";
      }
    });
  }

  // Show loading state on form submit
  if (form) {
    form.addEventListener("submit", function () {
      const submitBtn = document.querySelector(".btn-register");
      submitBtn.innerHTML = '<i class="bi bi-hourglass-split me-2"></i>Đang đăng ký...';
      submitBtn.disabled = true;
    });
  }
});
