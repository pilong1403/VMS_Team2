document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("changePasswordForm");
  const modal = document.getElementById("changePasswordModal");

  // Change password form handler
  if (form) {
    form.addEventListener("submit", handlePasswordChange);

    // Real-time validation
    const newPasswordField = document.getElementById("newPassword");
    const confirmPasswordField = document.getElementById("confirmPassword");

    if (newPasswordField) {
      newPasswordField.addEventListener("input", validateNewPassword);
    }

    if (confirmPasswordField) {
      confirmPasswordField.addEventListener("input", validateConfirmPassword);
    }
  }

  // Reset form when modal closes
  if (modal) {
    modal.addEventListener("hidden.bs.modal", () => {
      form.reset();
      clearFormErrors();
      // Hide password strength indicator
      const strengthContainer = document.querySelector(".password-strength");
      if (strengthContainer) {
        strengthContainer.classList.add("d-none");
      }
    });
  }

  function handlePasswordChange(e) {
    e.preventDefault();

    const currentPassword = document.getElementById("currentPassword").value;
    const newPassword = document.getElementById("newPassword").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    clearFormErrors();

    if (!validatePasswords(currentPassword, newPassword, confirmPassword)) {
      return;
    }

    toggleLoading(true);

    fetch(form.action, {
      method: "POST",
      body: new FormData(form),
      headers: { "X-Requested-With": "XMLHttpRequest" },
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.success) {
          bootstrap.Modal.getInstance(modal).hide();
          showSuccessToast("Đổi mật khẩu thành công");
          form.reset();
        } else {
          if (data.errors) {
            Object.entries(data.errors).forEach(([field, message]) => {
              showFieldError(field, message);
            });
          } else {
            showErrorToast(data.message || "Có lỗi xảy ra khi đổi mật khẩu");
          }
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        showErrorToast("Có lỗi xảy ra khi đổi mật khẩu");
      })
      .finally(() => toggleLoading(false));
  }

  function validatePasswords(current, newPass, confirm) {
    let isValid = true;

    if (!current) {
      showFieldError("currentPassword", "Vui lòng nhập mật khẩu hiện tại");
      isValid = false;
    }

    if (!newPass) {
      showFieldError("newPassword", "Vui lòng nhập mật khẩu mới");
      isValid = false;
    } else if (newPass.length < 8) {
      showFieldError("newPassword", "Mật khẩu phải có ít nhất 8 ký tự");
      isValid = false;
    } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(newPass)) {
      showFieldError("newPassword", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số");
      isValid = false;
    } else if (current === newPass) {
      showFieldError("newPassword", "Mật khẩu mới phải khác mật khẩu hiện tại");
      isValid = false;
    }

    if (!confirm) {
      showFieldError("confirmPassword", "Vui lòng xác nhận mật khẩu mới");
      isValid = false;
    } else if (newPass !== confirm) {
      showFieldError("confirmPassword", "Mật khẩu xác nhận không khớp");
      isValid = false;
    }

    return isValid;
  }

  function showFieldError(fieldId, message) {
    const field = document.getElementById(fieldId);
    const feedback = field.parentNode.querySelector(".invalid-feedback");

    field.classList.add("is-invalid");
    if (feedback) feedback.textContent = message;
  }

  function clearFormErrors() {
    form.querySelectorAll(".is-invalid").forEach((field) => {
      field.classList.remove("is-invalid");
    });
    form.querySelectorAll(".invalid-feedback").forEach((feedback) => {
      feedback.textContent = "";
    });
  }

  function toggleLoading(show) {
    const submitBtn = form.querySelector('button[type="submit"]');
    const spinner = submitBtn.querySelector(".spinner-border");

    spinner.classList.toggle("d-none", !show);
    submitBtn.disabled = show;
  }

  function validateNewPassword() {
    const newPassword = document.getElementById("newPassword").value;
    const currentPassword = document.getElementById("currentPassword").value;

    clearFieldError("newPassword");
    updatePasswordStrength(newPassword);

    if (newPassword.length > 0) {
      if (newPassword.length < 8) {
        showFieldError("newPassword", "Mật khẩu phải có ít nhất 8 ký tự");
      } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(newPassword)) {
        showFieldError("newPassword", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số");
      } else if (currentPassword && currentPassword === newPassword) {
        showFieldError("newPassword", "Mật khẩu mới phải khác mật khẩu hiện tại");
      }
    }

    // Re-validate confirm password if it has value
    const confirmPassword = document.getElementById("confirmPassword").value;
    if (confirmPassword.length > 0) {
      validateConfirmPassword();
    }
  }

  function updatePasswordStrength(password) {
    const strengthContainer = document.querySelector(".password-strength");
    const progressBar = strengthContainer?.querySelector(".progress-bar");
    const strengthText = strengthContainer?.querySelector(".strength-text");

    if (!strengthContainer) return;

    if (password.length === 0) {
      strengthContainer.classList.add("d-none");
      return;
    }

    strengthContainer.classList.remove("d-none");

    let strength = 0;
    let strengthLabel = "";
    let strengthClass = "";

    // Length check
    if (password.length >= 8) strength += 25;

    // Lowercase check
    if (/[a-z]/.test(password)) strength += 25;

    // Uppercase check
    if (/[A-Z]/.test(password)) strength += 25;

    // Number check
    if (/\d/.test(password)) strength += 25;

    // Special character bonus
    if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) strength += 10;

    // Length bonus
    if (password.length >= 12) strength += 10;

    // Determine strength level
    if (strength < 50) {
      strengthLabel = "Yếu";
      strengthClass = "bg-danger";
    } else if (strength < 75) {
      strengthLabel = "Trung bình";
      strengthClass = "bg-warning";
    } else if (strength < 100) {
      strengthLabel = "Mạnh";
      strengthClass = "bg-info";
    } else {
      strengthLabel = "Rất mạnh";
      strengthClass = "bg-success";
    }

    if (progressBar) {
      progressBar.style.width = Math.min(strength, 100) + "%";
      progressBar.className = `progress-bar ${strengthClass}`;
    }

    if (strengthText) {
      strengthText.textContent = `Độ mạnh: ${strengthLabel}`;
    }
  }

  function validateConfirmPassword() {
    const newPassword = document.getElementById("newPassword").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    clearFieldError("confirmPassword");

    if (confirmPassword.length > 0 && newPassword !== confirmPassword) {
      showFieldError("confirmPassword", "Mật khẩu xác nhận không khớp");
    }
  }

  function clearFieldError(fieldId) {
    const field = document.getElementById(fieldId);
    const feedback = field.parentNode.querySelector(".invalid-feedback");

    field.classList.remove("is-invalid");
    if (feedback) feedback.textContent = "";
  }
});
