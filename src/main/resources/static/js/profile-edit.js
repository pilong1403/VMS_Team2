document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("editProfileForm");
  const avatarUpload = document.getElementById("avatarUpload");
  const avatarPreview = document.getElementById("avatarPreview");
  const saveButton = document.getElementById("saveButton");

  // Avatar upload
  if (avatarUpload) {
    avatarUpload.addEventListener("change", handleAvatarUpload);
  }

  // Form submission
  if (form) {
    form.addEventListener("submit", handleFormSubmit);

    // Real-time validation
    form.querySelectorAll("input, textarea").forEach((input) => {
      input.addEventListener("blur", () => validateField(input));
      input.addEventListener("input", () => clearFieldError(input));
    });
  }

  // Avatar upload handler
  function handleAvatarUpload(e) {
    const file = e.target.files[0];
    if (!file) return;

    // Validate file
    if (!file.type.startsWith("image/")) {
      showErrorToast("Vui lòng chọn file hình ảnh hợp lệ");
      avatarUpload.value = "";
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      showErrorToast("Kích thước file không được vượt quá 5MB");
      avatarUpload.value = "";
      return;
    }

    // Preview image
    const reader = new FileReader();
    reader.onload = (e) => {
      avatarPreview.src = e.target.result;
      showUploadIndicator();
    };
    reader.readAsDataURL(file);
  }

  // Form submission handler
  function handleFormSubmit(e) {
    e.preventDefault();

    if (!validateForm()) return;

    toggleLoading(true);

    const formData = new FormData(form);
    if (avatarUpload.files[0]) {
      formData.set("avatarFile", avatarUpload.files[0]);
    }

    fetch("/profile/update", {
      method: "POST",
      body: formData,
      headers: { "X-Requested-With": "XMLHttpRequest" },
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.success) {
          showSuccessToast(data.message || "Cập nhật thành công");
          setTimeout(() => (window.location.href = "/profile"), 1500);
        } else {
          if (data.errors) {
            Object.entries(data.errors).forEach(([field, message]) => {
              showFieldError(form.querySelector(`[name="${field}"]`), message);
            });
          } else {
            showErrorToast(data.message || "Có lỗi xảy ra");
          }
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        form.action = "/profile/update-form";
        form.submit();
      })
      .finally(() => toggleLoading(false));
  }

  // Validation
  function validateForm() {
    const fullName = form.querySelector('[name="fullName"]');
    const email = form.querySelector('[name="email"]');
    const phone = form.querySelector('[name="phone"]');

    let isValid = true;
    if (!validateField(fullName)) isValid = false;
    if (!validateField(email)) isValid = false;
    if (phone.value && !validateField(phone)) isValid = false;

    return isValid;
  }

  function validateField(field) {
    const value = field.value.trim();
    let isValid = true;
    let message = "";

    switch (field.name) {
      case "fullName":
        if (!value) {
          message = "Họ và tên không được để trống";
          isValid = false;
        }
        break;
      case "email":
        if (!value) {
          message = "Email không được để trống";
          isValid = false;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
          message = "Email không hợp lệ";
          isValid = false;
        }
        break;
      case "phone":
        if (value && !/^[0-9]{10,11}$/.test(value.replace(/\s/g, ""))) {
          message = "Số điện thoại phải có 10-11 chữ số";
          isValid = false;
        }
        break;
    }

    if (!isValid) {
      showFieldError(field, message);
    } else {
      clearFieldError(field);
    }

    return isValid;
  }

  // UI helpers
  function showFieldError(field, message) {
    field.classList.add("is-invalid");
    const feedback = field.parentNode.querySelector(".invalid-feedback");
    if (feedback) feedback.textContent = message;
  }

  function clearFieldError(field) {
    field.classList.remove("is-invalid");
    const feedback = field.parentNode.querySelector(".invalid-feedback");
    if (feedback) feedback.textContent = "";
  }

  function toggleLoading(show) {
    const spinner = saveButton?.querySelector(".spinner-border");
    if (spinner) spinner.classList.toggle("d-none", !show);
    if (saveButton) saveButton.disabled = show;
  }

  function showUploadIndicator() {
    const indicator = document.createElement("div");
    indicator.className = "position-absolute top-50 start-50 translate-middle bg-primary text-white px-2 py-1 rounded";
    indicator.innerHTML = '<i class="bi bi-cloud-upload me-1"></i>Sẵn sàng upload';

    const existing = document.getElementById("uploadIndicator");
    if (existing) existing.remove();

    indicator.id = "uploadIndicator";
    document.querySelector(".avatar-container").appendChild(indicator);

    setTimeout(() => indicator.remove(), 2000);
  }

  // Check URL params for success message
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get("updated") === "true") {
    showSuccessToast("Cập nhật thành công");
    window.history.replaceState({}, document.title, window.location.pathname);
  }
});
