// Opportunities page JavaScript - Minimal

document.addEventListener("DOMContentLoaded", function () {
  // Filter functionality
  const applyButton = document.querySelector(".btn-primary-custom");
  const clearButton = document.querySelector('a[href="#"]');

  if (applyButton) {
    applyButton.addEventListener("click", function (e) {
      e.preventDefault();
      // Simulate filter application
      console.log("Filters applied");
    });
  }

  if (clearButton && clearButton.textContent.includes("Xóa Bộ Lọc")) {
    clearButton.addEventListener("click", function (e) {
      e.preventDefault();
      // Clear all form inputs
      document.querySelectorAll('input[type="checkbox"]').forEach((cb) => (cb.checked = false));
      document.querySelectorAll('input[type="radio"]').forEach((rb) => (rb.checked = false));
      document.querySelectorAll('input[type="text"]').forEach((input) => (input.value = ""));
      document.querySelectorAll("select").forEach((select) => (select.selectedIndex = 0));
    });
  }

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
