// Global variables
let currentOppId = null;
let currentFeedbackId = null;
let selectedStars = 0;
let isEditMode = false;

// Open rating modal from data attributes
function openRatingModalFromData(button) {
  const oppId = parseInt(button.dataset.oppId);
  const eventTitle = button.dataset.eventTitle;
  openModal(oppId, eventTitle, false);
}

function editFeedbackFromData(button) {
  const feedbackId = parseInt(button.dataset.feedbackId);
  const rating = parseInt(button.dataset.rating);
  const content = button.dataset.content;
  openModal(feedbackId, "Chỉnh sửa đánh giá", true, rating, content);
}

// Unified modal opening function
function openModal(id, title, editMode = false, rating = 0, content = "") {
  isEditMode = editMode;
  selectedStars = rating;

  if (editMode) {
    currentFeedbackId = id;
    currentOppId = null;
  } else {
    currentOppId = id;
    currentFeedbackId = null;
  }

  document.getElementById("modalEventTitle").textContent = title;
  document.getElementById("ratingComment").value = content;
  document.getElementById("submitRating").textContent = editMode ? "Cập nhật đánh giá" : "Gửi đánh giá";

  // Set stars
  document.querySelectorAll(".star").forEach((star, index) => {
    star.classList.toggle("active", index < rating);
  });

  new bootstrap.Modal(document.getElementById("ratingModal")).show();
}

// Initialize star rating functionality
function initializeStarRating() {
  const stars = document.querySelectorAll(".star");

  stars.forEach((star) => {
    star.addEventListener("click", function () {
      selectedStars = parseInt(this.dataset.rating);
      updateStars(selectedStars);
    });

    star.addEventListener("mouseenter", function () {
      const rating = parseInt(this.dataset.rating);
      updateStars(rating, true);
    });
  });

  document.getElementById("starRating").addEventListener("mouseleave", () => {
    updateStars(selectedStars, true);
  });
}

// Update star display
function updateStars(rating, isHover = false) {
  document.querySelectorAll(".star").forEach((star, index) => {
    if (!isHover) {
      star.classList.toggle("active", index < rating);
    }
    star.style.color = index < rating ? "#ffc107" : "#ddd";
  });
}

// Submit rating/feedback via AJAX
function submitRating() {
  if (selectedStars === 0) {
    showWarningToast("Vui lòng chọn số sao đánh giá!");
    return;
  }

  const submitBtn = document.getElementById("submitRating");
  const originalText = submitBtn.textContent;
  submitBtn.disabled = true;
  submitBtn.textContent = "Đang gửi...";

  const formData = new FormData();
  formData.append(isEditMode ? "feedbackId" : "oppId", isEditMode ? currentFeedbackId : currentOppId);
  formData.append("rating", selectedStars);
  formData.append("content", document.getElementById("ratingComment").value.trim());

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");

  fetch(isEditMode ? "/profile/update-feedback" : "/profile/rate-event", {
    method: "POST",
    headers: csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {},
    body: formData,
  })
    .then((response) => response.text())
    .then((result) => {
      if (result.startsWith("success:")) {
        showSuccessToast(result.substring(8));
        setTimeout(() => location.reload(), 1500);
      } else {
        showErrorToast(result.startsWith("error:") ? result.substring(6) : "Đã xảy ra lỗi không xác định");
      }
    })
    .catch((error) => {
      console.error("Error:", error);
      showErrorToast("Đã xảy ra lỗi khi gửi đánh giá");
    })
    .finally(() => {
      submitBtn.disabled = false;
      submitBtn.textContent = originalText;
    });
}

// Render star displays based on data-rating attribute
function renderStarDisplays() {
  document.querySelectorAll(".star-display").forEach((starDisplay) => {
    const rating = parseInt(starDisplay.dataset.rating);
    if (rating > 0) {
      starDisplay.innerHTML = "★".repeat(rating) + "☆".repeat(5 - rating);
    }
  });
}

// Initialize when DOM is loaded
document.addEventListener("DOMContentLoaded", function () {
  renderStarDisplays();
  initializeStarRating();

  const submitButton = document.getElementById("submitRating");
  if (submitButton) {
    submitButton.addEventListener("click", submitRating);
  }
});
