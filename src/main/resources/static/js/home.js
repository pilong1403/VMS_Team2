// Minimal JavaScript - Desktop Only

document.addEventListener("DOMContentLoaded", function () {
  // Basic carousel initialization
  const heroCarousel = document.getElementById("heroCarousel");
  if (heroCarousel) {
    new bootstrap.Carousel(heroCarousel, {
      interval: 5000,
      wrap: true,
    });
  }

  // Simple registration button handler
  if (!document.getElementById("applyModal")) {
    document.querySelectorAll(".btn-primary-custom").forEach((button) => {
      if (button.textContent.includes("Đăng Ký")) {
        button.addEventListener("click", function (e) {
          e.preventDefault();
          alert("Vui lòng đăng nhập để đăng ký tham gia cơ hội.");
        });
      }
    });
  }

  // [THÊM MỚI] Open Apply Modal on Home + VALIDATION
  (function attachApplyModalHandler() {
    const modalEl = document.getElementById("applyModal");
    if (!modalEl) return; // nếu trang không có modal thì bỏ qua

    const modal = new bootstrap.Modal(modalEl);
    const form = modalEl.querySelector("form.needs-validation");

    const oppIdInput = modalEl.querySelector('input[name="oppId"]');
    const fullNameInput = modalEl.querySelector('input[name="fullName"]');
    const emailInput = modalEl.querySelector('input[name="email"], input[type="email"]');
    const phoneInput = modalEl.querySelector('input[name="phone"]');
    const addressInput = modalEl.querySelector('input[name="address"]');
    const reasonInput = modalEl.querySelector('textarea[name="reason"]');

    // Bắt các nút "Đăng Ký" đã gắn data-opp-id
    document.querySelectorAll('.btn-primary-custom[data-opp-id]').forEach((btn) => {
      btn.addEventListener("click", function (e) {
        e.preventDefault();

        // Nếu chưa đăng nhập → chuyển tới login
        if (!window.CURRENT_USER_ID) {
          window.location.href = "/login?e=USERNAME_PASSWORD_REQUIRED";
          return;
        }

        const oppId = btn.getAttribute("data-opp-id");
        if (oppIdInput) oppIdInput.value = (oppId || "").trim();

        // Điền sẵn thông tin user nếu có
        if (window.CURRENT_USER) {
          const u = window.CURRENT_USER;
          if (fullNameInput) fullNameInput.value = u.fullName || "";
          if (emailInput) emailInput.value = u.email || "";
          if (phoneInput) phoneInput.value = (u.phone || "").replace(/\D/g, "").slice(0, 10);
          if (addressInput) addressInput.value = u.address || "";
        }

        // Reset trạng thái validate UI
        if (form) form.classList.remove("was-validated");

        modal.show();
      });
    });

    if (!form) return;

    // Đảm bảo tất cả trường là bắt buộc
    [fullNameInput, emailInput, phoneInput, addressInput, reasonInput].forEach((el) => {
      if (el) el.setAttribute("required", "required");
    });

    // Chuẩn hoá & giới hạn phone theo thời gian thực
    if (phoneInput) {
      // set pattern & length (phòng trường hợp chưa có trong HTML)
      phoneInput.setAttribute("pattern", "^0\\d{9}$");
      phoneInput.setAttribute("minlength", "10");
      phoneInput.setAttribute("maxlength", "10");
      phoneInput.setAttribute("inputmode", "numeric");

      phoneInput.addEventListener("input", () => {
        phoneInput.value = phoneInput.value.replace(/\D/g, "").slice(0, 10);
        // Xoá custom error khi người dùng đang sửa
        phoneInput.setCustomValidity("");
      });
    }

    // Submit validation
    form.addEventListener("submit", function (event) {
      // 1) oppId phải có
      if (!oppIdInput || !oppIdInput.value || !oppIdInput.value.trim()) {
        event.preventDefault();
        event.stopPropagation();
        alert("Không xác định được cơ hội. Vui lòng bấm lại nút Đăng ký.");
        return;
      }

      // 2) Chuẩn hoá phone lần cuối & kiểm tra regex
      if (phoneInput) {
        phoneInput.value = (phoneInput.value || "").replace(/\D/g, "").slice(0, 10);
        const phoneOk = /^0\d{9}$/.test(phoneInput.value);
        if (!phoneOk) {
          phoneInput.setCustomValidity("Số điện thoại không hợp lệ");
        } else {
          phoneInput.setCustomValidity("");
        }
      }

      // 3) Kiểm tra các trường required khác (tránh user xoá thuộc tính required từ DevTools)
      const mustFilled = [
        { el: fullNameInput, min: 2 },
        { el: emailInput, min: 3 },
        { el: addressInput, min: 2 },
        { el: reasonInput, min: 1 },
      ];
      mustFilled.forEach(({ el, min }) => {
        if (!el) return;
        const v = (el.value || "").trim();
        if (v.length < (min || 1)) {
          el.setCustomValidity("Vui lòng điền thông tin bắt buộc");
        } else {
          el.setCustomValidity("");
        }
      });

      // 4) Nếu form không hợp lệ thì chặn submit
      if (!form.checkValidity()) {
        event.preventDefault();
        event.stopPropagation();
      }

      // 5) Bootstrap hiển thị UI validate
      form.classList.add("was-validated");
    });
  })();

});
