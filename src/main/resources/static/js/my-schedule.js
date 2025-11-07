let currentEventData = {};

function initializeMySchedule() {
  console.log("My Schedule page initialized");
}

function addToGoogleCalendar(button) {
  event.stopPropagation();

  const title = button.getAttribute("data-title");
  const startTime = button.getAttribute("data-start");
  const endTime = button.getAttribute("data-end");
  const location = button.getAttribute("data-location");
  const organization = button.getAttribute("data-organization");

  createGoogleCalendarUrl(title, startTime, endTime, location, organization);
}

function addToGoogleCalendarFromModal() {
  const { title, startTime, endTime, location, organization } = currentEventData;
  createGoogleCalendarUrl(title, startTime, endTime, location, organization);
}

function createGoogleCalendarUrl(title, startTime, endTime, location, organization) {
  const formatDateForGoogle = (dateString) => {
    const date = new Date(dateString);
    return date
      .toISOString()
      .replace(/[-:]/g, "")
      .replace(/\.\d{3}/, "");
  };

  const startFormatted = formatDateForGoogle(startTime);
  const endFormatted = formatDateForGoogle(endTime);

  const description = `Hoạt động tình nguyện được tổ chức bởi ${organization}`;

  const googleCalendarUrl =
    "https://calendar.google.com/calendar/render?" +
    "action=TEMPLATE" +
    "&text=" +
    encodeURIComponent(title) +
    "&dates=" +
    startFormatted +
    "/" +
    endFormatted +
    "&location=" +
    encodeURIComponent(location) +
    "&details=" +
    encodeURIComponent(description);

  window.open(googleCalendarUrl, "_blank");
}

function showEventDetail(eventCard) {
  const eventData = extractEventData(eventCard);

  currentEventData = {
    title: eventData.title,
    startTime: eventData.startTime,
    endTime: eventData.endTime,
    location: eventData.location,
    organization: eventData.organization,
  };

  populateModal(eventData);

  const modal = new bootstrap.Modal(document.getElementById("eventDetailModal"));
  modal.show();
}

function extractEventData(eventCard) {
  return {
    appId: eventCard.getAttribute("data-app-id"),
    title: eventCard.getAttribute("data-title"),
    startTime: eventCard.getAttribute("data-start"),
    endTime: eventCard.getAttribute("data-end"),
    location: eventCard.getAttribute("data-location"),
    organization: eventCard.getAttribute("data-organization"),
    description: eventCard.getAttribute("data-description"),
    appliedAt: eventCard.getAttribute("data-applied-at"),
    status: eventCard.getAttribute("data-status"),
    neededVolunteers: eventCard.getAttribute("data-needed-volunteers"),
    thumbnailUrl: eventCard.getAttribute("data-thumbnail"),
  };
}

function populateModal(eventData) {
  const startDate = new Date(eventData.startTime);
  const endDate = new Date(eventData.endTime);
  const appliedDate = new Date(eventData.appliedAt);

  updateModalContent(eventData, startDate, endDate, appliedDate);
  updateModalThumbnail(eventData.thumbnailUrl);
  updateModalStatus(eventData.status);
  updateModalCountdown(startDate);
}

function updateModalContent(eventData, startDate, endDate, appliedDate) {
  document.getElementById("modal-event-title").textContent = eventData.title;
  document.getElementById("modal-organization").textContent = eventData.organization;
  document.getElementById("modal-date").textContent = formatDate(startDate);
  document.getElementById("modal-time").textContent = `${formatTime(startDate)} - ${formatTime(endDate)}`;
  document.getElementById("modal-location").textContent = eventData.location;
  document.getElementById("modal-description").textContent = eventData.description || "Chưa có mô tả chi tiết";
  document.getElementById("modal-needed-volunteers").textContent = eventData.neededVolunteers || "0";
  document.getElementById("modal-applied-at").textContent = formatDateTime(appliedDate);
  document.getElementById("modal-app-id").textContent = eventData.appId;
}

function updateModalThumbnail(thumbnailUrl) {
  const thumbnailContainer = document.getElementById("modal-thumbnail-container");
  const thumbnailImg = document.getElementById("modal-thumbnail");

  if (thumbnailUrl && thumbnailUrl.trim() !== "") {
    thumbnailImg.src = thumbnailUrl;
    thumbnailContainer.style.display = "block";
  } else {
    thumbnailContainer.style.display = "none";
  }
}

function updateModalStatus(status) {
  const statusElement = document.getElementById("modal-status");
  const statusText = getStatusText(status);
  const statusClass = getStatusClass(status);

  statusElement.textContent = statusText;
  statusElement.className = `status-badge ${statusClass}`;
}

function updateModalCountdown(startDate) {
  const now = new Date();
  const timeDiff = startDate - now;
  const daysLeft = Math.ceil(timeDiff / (1000 * 60 * 60 * 24));

  const countdownElement = document.getElementById("modal-countdown");

  if (daysLeft > 0) {
    countdownElement.textContent = `Còn ${daysLeft} ngày`;
    countdownElement.className = "countdown upcoming";
  } else if (daysLeft === 0) {
    countdownElement.textContent = "Hôm nay";
    countdownElement.className = "countdown today";
  } else {
    countdownElement.textContent = "Đã qua";
    countdownElement.className = "countdown past";
  }
}

function getStatusText(status) {
  const statusMap = {
    APPROVED: "Đã duyệt",
    PENDING: "Chờ duyệt",
    REJECTED: "Từ chối",
    COMPLETED: "Hoàn thành",
    CANCELLED: "Đã hủy",
  };

  return statusMap[status] || "Sắp diễn ra";
}

function getStatusClass(status) {
  const statusClassMap = {
    APPROVED: "approved",
    PENDING: "pending",
    REJECTED: "rejected",
    COMPLETED: "completed",
    CANCELLED: "cancelled",
  };

  return statusClassMap[status] || "upcoming";
}

function formatDate(date) {
  return date.toLocaleDateString("vi-VN", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

function formatTime(date) {
  return date.toLocaleTimeString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatDateTime(date) {
  return date.toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

document.addEventListener("DOMContentLoaded", function () {
  initializeMySchedule();
});
