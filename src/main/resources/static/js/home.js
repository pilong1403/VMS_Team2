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
  document.querySelectorAll(".btn-primary-custom").forEach((button) => {
    if (button.textContent.includes("Đăng Ký")) {
      button.addEventListener("click", function (e) {
        e.preventDefault();
        alert("Vui lòng đăng nhập để đăng ký tham gia cơ hội.");
      });
    }
  });
});
