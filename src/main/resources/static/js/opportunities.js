// Opportunities page JavaScript

document.addEventListener("DOMContentLoaded", function () {
  // Registration button handlers
  document.querySelectorAll(".btn-primary-custom").forEach((button) => {
    if (button.textContent.includes("Đăng Ký")) {
      button.addEventListener("click", function (e) {
        e.preventDefault();
        alert("Vui lòng đăng nhập để đăng ký tham gia cơ hội.");
      });
    }
  });
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

  // Get selected location
  const locationSelect = document.querySelector('select[name="location"]');
  if (locationSelect && locationSelect.value) {
    params.append("location", locationSelect.value);
  }

  // Get selected status
  const selectedStatus = document.querySelector('input[name="status"]:checked');
  if (selectedStatus && selectedStatus.value) {
    params.append("status", selectedStatus.value);
  }

  // Get sort option
  const sortSelect = document.querySelector('select[name="sort"]');
  if (sortSelect && sortSelect.value) {
    params.append("sort", sortSelect.value);
  }

  // Reset to first page
  params.append("page", "0");
  params.append("size", "6");

  // Navigate to filtered URL
  window.location.href = "/opportunities?" + params.toString();
}
