document.addEventListener("DOMContentLoaded", function () {
  const form = document.querySelector(".login-form");
  const emailInput = document.getElementById("email");
  const passwordInput = document.getElementById("password");
  const togglePasswordBtn = document.getElementById("togglePassword");
  const toggleIcon = document.getElementById("toggleIcon");

  // Các khối lỗi có thể xuất hiện
  const inlineError = document.getElementById("inlineError");         // lỗi client-side
  const serverError = document.getElementById("serverError");         // ${error}
  const serverParamError = document.getElementById("serverParamError"); // ${param.error}

  // ===== Helpers ===========================================================
  function hideEl(el) {
    if (!el) return;
    el.textContent = el.id === "inlineError" ? "" : el.textContent; // inlineError clear text
    el.classList.add("hidden");
    el.removeAttribute("role");
    el.removeAttribute("aria-live");
  }

  function showInlineError(message, focusField) {
    if (inlineError) {
      inlineError.textContent = message || "Đã xảy ra lỗi.";
      inlineError.classList.remove("hidden");
      inlineError.setAttribute("role", "alert");
      inlineError.setAttribute("aria-live", "assertive");
    } else {
      alert(message || "Đã xảy ra lỗi.");
    }
    if (focusField && focusField.focus) {
      focusField.classList.add("border-red-500", "ring", "ring-red-300");
      focusField.focus();
    }
  }

  function clearFieldErrorStyles() {
    [emailInput, passwordInput].forEach((el) => {
      if (el) el.classList.remove("border-red-500", "ring", "ring-red-300");
    });
  }

  function hideAllErrors() {
    hideEl(inlineError);
    hideEl(serverError);
    hideEl(serverParamError);
    clearFieldErrorStyles();
  }

  // ===== Toggle password ===================================================
  function togglePasswordVisibility() {
    if (!passwordInput) return;
    const isHidden = passwordInput.type === "password";
    passwordInput.type = isHidden ? "text" : "password";
    if (toggleIcon) toggleIcon.textContent = isHidden ? "Ẩn" : "Hiện";
  }

  if (togglePasswordBtn) {
    togglePasswordBtn.addEventListener("click", function (e) {
      e.preventDefault();
      togglePasswordVisibility();
    });
  }

  // ===== Ẩn lỗi khi người dùng bắt đầu sửa input ===========================
  function wireHideOnInteract(el) {
    if (!el) return;
    el.addEventListener("focus", hideAllErrors);
    el.addEventListener("input", hideAllErrors);
  }
  wireHideOnInteract(emailInput);
  wireHideOnInteract(passwordInput);

  // Dự phòng: nếu không muốn chỉnh HTML thêm id, có thể fallback ẩn các alert đỏ trên trang
  // (để an toàn, chỉ bật khi không có id đã gán)
  if (!serverError && !serverParamError) {
    const genericAlerts = document.querySelectorAll(
        ".bg-red-50.p-3.text-red-700"
    );
    function hideGenericAlerts() {
      genericAlerts.forEach((el) => el.classList.add("hidden"));
    }
    if (emailInput) {
      emailInput.addEventListener("focus", hideGenericAlerts);
      emailInput.addEventListener("input", hideGenericAlerts);
    }
    if (passwordInput) {
      passwordInput.addEventListener("focus", hideGenericAlerts);
      passwordInput.addEventListener("input", hideGenericAlerts);
    }
  }

  // ===== Validate & submit =================================================
  if (form) {
    form.addEventListener("submit", function (e) {
      hideAllErrors();

      const email = emailInput ? emailInput.value.trim() : "";
      const password = passwordInput ? passwordInput.value : "";

      if (!email || !password) {
        e.preventDefault();
        if (!email && emailInput) {
          showInlineError("Vui lòng nhập đầy đủ thông tin đăng nhập.", emailInput);
        } else if (!password && passwordInput) {
          showInlineError("Vui lòng nhập đầy đủ thông tin đăng nhập.", passwordInput);
        } else {
          showInlineError("Vui lòng nhập đầy đủ thông tin đăng nhập.");
        }
        return;
      }

      const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRe.test(email)) {
        e.preventDefault();
        showInlineError("Vui lòng nhập email hợp lệ.", emailInput);
        return;
      }

      const submitBtn = document.querySelector(".btn-login");
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = "Đang đăng nhập...";
      }
    });
  }
});
