// Opportunities page JavaScript

document.addEventListener("DOMContentLoaded", function () {
  // ====== Đăng ký tham gia (Apply) ======
  // YÊU CẦU: trong trang HTML có biến window.CURRENT_USER_ID (lấy từ session)
  // và có modal #applyModal (form POST /applications/apply, hidden input name="oppId")
  const applyButtons = document.querySelectorAll('.js-apply-btn[data-opp-id]');

  applyButtons.forEach((btn) => {
    btn.addEventListener("click", function (e) {
      e.preventDefault();

      // Chưa đăng nhập → chuyển sang trang đăng nhập
      if (!window.CURRENT_USER_ID) {
        window.location.href = "/login?e=USERNAME_PASSWORD_REQUIRED";
        return;
      }

      const oppId = this.getAttribute("data-opp-id");
      const modalEl = document.getElementById("applyModal");
      if (!modalEl) {
        console.warn("Không tìm thấy #applyModal trên trang.");
        return;
      }

      // Gắn oppId vào hidden input của form
      const oppInput = modalEl.querySelector('input[name="oppId"]');
      if (oppInput) oppInput.value = oppId;

      // Mở modal
      const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
      modal.show();
    });
  });

  // ====== Enter để áp dụng bộ lọc ======
  const searchInput = document.querySelector('input[name="search"]');
  const locationInput = document.querySelector('input[name="location"]');

  if (searchInput) {
    searchInput.addEventListener("keypress", function (e) {
      if (e.key === "Enter") {
        e.preventDefault();
        applyFilters();
      }
    });
  }

  if (locationInput) {
    locationInput.addEventListener("keypress", function (e) {
      if (e.key === "Enter") {
        e.preventDefault();
        applyFilters();
      }
    });
  }
});

// Apply filters function
function applyFilters() {
  const params = new URLSearchParams();

  // Get search term
  const searchInput = document.querySelector('input[name="search"]');
  if (searchInput && searchInput.value.trim()) {
    params.append("search", searchInput.value.trim());
  }

  // Get selected category
  const selectedCategory = document.querySelector('input[name="category"]:checked');
  if (selectedCategory && selectedCategory.value) {
    params.append("categoryId", selectedCategory.value);
  }

  // Get location input
  const locationInput = document.querySelector('input[name="location"]');
  if (locationInput && locationInput.value.trim()) {
    params.append("location", locationInput.value.trim());
  }

  // Get selected time filter
  const selectedTime = document.querySelector('input[name="time"]:checked');
  if (selectedTime && selectedTime.value) {
    params.append("time", selectedTime.value);
  }

  // Get selected status
  const selectedStatus = document.querySelector('input[name="status"]:checked');
  if (selectedStatus && selectedStatus.value) {
    params.append("status", selectedStatus.value);
  }

  // Reset to first page
  params.append("page", "0");
  params.append("size", "6");

  // Navigate to filtered URL
  window.location.href = "/opportunities?" + params.toString();
}

// Clear all filters function
function clearFilters() {
  // Clear search input
  const searchInput = document.querySelector('input[name="search"]');
  if (searchInput) searchInput.value = "";

  // Clear location input
  const locationInput = document.querySelector('input[name="location"]');
  if (locationInput) locationInput.value = "";

  // Reset category to "All"
  const categoryAll = document.querySelector("#catAll");
  if (categoryAll) categoryAll.checked = true;

  // Reset time to "All"
  const timeAll = document.querySelector("#timeAll");
  if (timeAll) timeAll.checked = true;

  // Reset status to "All"
  const statusAll = document.querySelector("#statusAll");
  if (statusAll) statusAll.checked = true;

  // Navigate to clean URL
  window.location.href = "/opportunities";
}
