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

document.addEventListener("DOMContentLoaded", function () {
    const gBtn = document.getElementById("btnGoogle");
    if (!gBtn) return;

    const googleUrl = gBtn.getAttribute("href");
    const googleModalEl = document.getElementById("googleConfirmModal");
    const googleModal = googleModalEl ? new bootstrap.Modal(googleModalEl) : null;
    const confirmBtn = document.getElementById("btnGoogleConfirm");

    // Chặn điều hướng ngay để hiển thị modal
    gBtn.addEventListener("click", function (e) {
        if (googleModal) {
            e.preventDefault();
            googleModal.show();
        }
    });

    // Người dùng xác nhận -> chuyển hướng sang Google
    if (confirmBtn) {
        confirmBtn.addEventListener("click", function () {
            // loading trạng thái trên nút Google (tùy ý)
            gBtn.setAttribute("aria-disabled", "true");
            gBtn.innerHTML = `
        <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        Đang chuyển hướng...
      `;
            googleModal.hide();
            window.location.href = googleUrl;
        });
    }

    // Nếu backend chặn và trả e=OAUTH_ROLE_BLOCKED -> show cảnh báo
    const params = new URLSearchParams(window.location.search);
    if (params.get("e") === "OAUTH_ROLE_BLOCKED") {
        const form = document.querySelector(".login-form");
        if (form) {
            const alert = document.createElement("div");
            alert.className = "alert alert-warning mt-3";
            alert.innerHTML = `
        <i class="bi bi-exclamation-triangle me-2"></i>
        Chỉ <strong>Tình nguyện viên</strong> được đăng nhập bằng Google.
        <br/>Tổ chức/Admin hãy dùng <strong>email &amp; mật khẩu</strong>.
      `;
            form.prepend(alert);
        }
    }
});

