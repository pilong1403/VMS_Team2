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
  // [THÊM MỚI] Open Apply Modal on Home
  (function attachApplyModalHandler() {
    const modalEl = document.getElementById("applyModal");
    if (!modalEl) return; // nếu trang không có modal thì bỏ qua

    const modal = new bootstrap.Modal(modalEl);
    const oppIdInput = modalEl.querySelector('input[name="oppId"]');
    const fullNameInput = modalEl.querySelector('input[name="fullName"]');
    const emailInput = modalEl.querySelector('input[type="email"]');
    const phoneInput = modalEl.querySelector('input[name="phone"]');
    const addressInput = modalEl.querySelector('input[name="address"]');


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
        if (oppIdInput) oppIdInput.value = oppId;

        // Điền sẵn thông tin user nếu có
        if (window.CURRENT_USER) {
          const u = window.CURRENT_USER;
          if (fullNameInput) fullNameInput.value = u.fullName || "";
          if (emailInput) emailInput.value = u.email || "";
          if (phoneInput) phoneInput.value = u.phone || "";
          if (addressInput) addressInput.value = u.address || "";
        }
        modal.show();
      });
    });
  })();

});
