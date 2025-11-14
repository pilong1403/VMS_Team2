let currentEventData = {};
let currentDate = new Date();
let eventsData = [];
let currentView = "calendar";

function initializeMySchedule() {
  console.log("My Schedule page initialized");
  loadEventsData();
  renderCalendar();
}

function loadEventsData() {
  const eventElements = document.querySelectorAll("#events-data .event-data-item");
  eventsData = Array.from(eventElements).map((element) => ({
    appId: element.getAttribute("data-app-id"),
    title: element.getAttribute("data-title"),
    startTime: element.getAttribute("data-start"),
    endTime: element.getAttribute("data-end"),
    location: element.getAttribute("data-location"),
    organization: element.getAttribute("data-organization"),
    description: element.getAttribute("data-description"),
    appliedAt: element.getAttribute("data-applied-at"),
    status: element.getAttribute("data-status"),
    neededVolunteers: element.getAttribute("data-needed-volunteers"),
    thumbnailUrl: element.getAttribute("data-thumbnail"),
  }));
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

function switchToCalendarView() {
  currentView = "calendar";
  document.getElementById("calendar-view").style.display = "block";
  document.getElementById("list-view").style.display = "none";
  document.getElementById("statistics-cards").style.display = "none";
  document.getElementById("calendar-view-btn").className = "btn btn-primary";
  document.getElementById("list-view-btn").className = "btn btn-outline-secondary";
  renderCalendar();
}

function switchToListView() {
  currentView = "list";
  document.getElementById("calendar-view").style.display = "none";
  document.getElementById("list-view").style.display = "block";
  document.getElementById("statistics-cards").style.display = "flex";
  document.getElementById("list-view-btn").className = "btn btn-primary";
  document.getElementById("calendar-view-btn").className = "btn btn-outline-secondary";
}

function changeMonth(direction) {
  currentDate.setMonth(currentDate.getMonth() + direction);
  renderCalendar();
}

function renderCalendar() {
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  updateCalendarHeader(year, month);
  renderCalendarDays(year, month);
}

function updateCalendarHeader(year, month) {
  const monthNames = [
    "Tháng 1",
    "Tháng 2",
    "Tháng 3",
    "Tháng 4",
    "Tháng 5",
    "Tháng 6",
    "Tháng 7",
    "Tháng 8",
    "Tháng 9",
    "Tháng 10",
    "Tháng 11",
    "Tháng 12",
  ];

  document.getElementById("current-month-year").textContent = `${monthNames[month]}, ${year}`;
}

function renderCalendarDays(year, month) {
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const startDate = new Date(firstDay);
  startDate.setDate(startDate.getDate() - firstDay.getDay());

  const calendarDays = document.getElementById("calendar-days");
  calendarDays.innerHTML = "";

  for (let i = 0; i < 42; i++) {
    const currentDay = new Date(startDate);
    currentDay.setDate(startDate.getDate() + i);

    const dayElement = createDayElement(currentDay, month);
    calendarDays.appendChild(dayElement);
  }
}

function createDayElement(date, currentMonth) {
  const dayElement = document.createElement("div");
  dayElement.className = "calendar-day";

  const isCurrentMonth = date.getMonth() === currentMonth;
  const isToday = isDateToday(date);
  const dayEvents = getEventsForDate(date);

  if (!isCurrentMonth) {
    dayElement.classList.add("other-month");
  }

  if (isToday) {
    dayElement.classList.add("today");
  }

  if (dayEvents.length > 0) {
    dayElement.classList.add("has-events");
  }

  const dayNumber = document.createElement("div");
  dayNumber.className = "day-number";
  dayNumber.textContent = date.getDate();
  dayElement.appendChild(dayNumber);

  if (dayEvents.length > 0) {
    const eventsContainer = document.createElement("div");
    eventsContainer.className = "calendar-events";

    const maxVisible = 3;
    dayEvents.slice(0, maxVisible).forEach((event) => {
      const eventElement = createCalendarEventElement(event);
      eventsContainer.appendChild(eventElement);
    });

    if (dayEvents.length > maxVisible) {
      const moreElement = document.createElement("div");
      moreElement.className = "more-events";
      moreElement.textContent = `+${dayEvents.length - maxVisible} khác`;
      moreElement.onclick = () => showDayEvents(date, dayEvents);
      eventsContainer.appendChild(moreElement);
    }

    dayElement.appendChild(eventsContainer);
  }

  return dayElement;
}

function createCalendarEventElement(event) {
  const eventElement = document.createElement("div");
  eventElement.className = `calendar-event status-${event.status.toLowerCase()}`;
  eventElement.textContent = event.title;
  eventElement.onclick = (e) => {
    e.stopPropagation();
    showEventDetailFromData(event);
  };

  return eventElement;
}

function getEventsForDate(date) {
  return eventsData.filter((event) => {
    const eventDate = new Date(event.startTime);
    return eventDate.toDateString() === date.toDateString();
  });
}

function isDateToday(date) {
  const today = new Date();
  return date.toDateString() === today.toDateString();
}

function showDayEvents(date, events) {
  console.log(`Showing ${events.length} events for ${date.toDateString()}`);
}

function showEventDetailFromData(eventData) {
  currentEventData = {
    title: eventData.title,
    startTime: eventData.startTime,
    endTime: eventData.endTime,
    location: eventData.location,
    organization: eventData.organization,
  };

  populateModalFromData(eventData);

  const modal = new bootstrap.Modal(document.getElementById("eventDetailModal"));
  modal.show();
}

function populateModalFromData(eventData) {
  const startDate = new Date(eventData.startTime);
  const endDate = new Date(eventData.endTime);
  const appliedDate = new Date(eventData.appliedAt);

  document.getElementById("modal-event-title").textContent = eventData.title;
  document.getElementById("modal-organization").textContent = eventData.organization;
  document.getElementById("modal-date").textContent = formatDate(startDate);
  document.getElementById("modal-time").textContent = `${formatTime(startDate)} - ${formatTime(endDate)}`;
  document.getElementById("modal-location").textContent = eventData.location;
  document.getElementById("modal-description").textContent = eventData.description || "Chưa có mô tả chi tiết";
  document.getElementById("modal-needed-volunteers").textContent = eventData.neededVolunteers || "0";
  document.getElementById("modal-applied-at").textContent = formatDateTime(appliedDate);

  updateModalThumbnail(eventData.thumbnailUrl);
  updateModalStatus(eventData.status);
  updateModalCountdown(startDate);
}

document.addEventListener("DOMContentLoaded", function () {
  initializeMySchedule();

  // Check URL parameter to determine initial view
  const urlParams = new URLSearchParams(window.location.search);
  const viewParam = urlParams.get("view");

  if (viewParam === "list") {
    switchToListView();
  } else {
    switchToCalendarView();
  }
});
