document.addEventListener("DOMContentLoaded", function () {
  const form = document.querySelector(".login-form");
  const togglePasswordBtn = document.getElementById("togglePassword");
  const passwordInput = document.getElementById("password");
  const toggleIcon = document.getElementById("toggleIcon");

  // Toggle password visibility
  if (togglePasswordBtn) {
    togglePasswordBtn.addEventListener("click", togglePasswordVisibility);
  }

  // Form validation and submission
  if (form) {
    form.addEventListener("submit", handleFormSubmit);
  }

  function togglePasswordVisibility() {
    if (passwordInput.type === "password") {
      passwordInput.type = "text";
      toggleIcon.className = "bi bi-eye-slash";
    } else {
      passwordInput.type = "password";
      toggleIcon.className = "bi bi-eye";
    }
  }

  function handleFormSubmit(e) {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    // Basic validation
    if (!email || !password) {
      e.preventDefault();
      showErrorToast("Vui lòng nhập đầy đủ thông tin đăng nhập!");
      return false;
    }

    // Email validation
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      e.preventDefault();
      showErrorToast("Vui lòng nhập email hợp lệ!");
      return false;
    }

    // Show loading state
    const submitBtn = document.querySelector(".btn-login");
    submitBtn.innerHTML = '<i class="bi bi-hourglass-split me-2"></i>Đang đăng nhập...';
    submitBtn.disabled = true;
  }
});
