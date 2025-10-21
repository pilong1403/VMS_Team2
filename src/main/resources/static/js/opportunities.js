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

  // Add event listeners for form inputs to enable Enter key submission
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

  // Note: Removed auto-filtering on radio button change - users must click "Áp Dụng" button
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

  // Get location input (changed from select to input)
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

  // Sort removed - using default newest sort

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
