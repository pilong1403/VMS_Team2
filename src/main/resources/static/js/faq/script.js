function openCreateFAQModal() {
  document.getElementById("faqModalCreate").classList.add("show");
}

function closeCreateFAQModal() {
  document.getElementById("faqModalCreate").classList.remove("show");
}

function closeEditFAQModal() {
    document.getElementById("faqModalEdit").classList.remove("show");
}

// Simple pagination active state
document.querySelectorAll(".page-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
        if (btn.disabled) return;
        document
            .querySelectorAll(".page-btn")
            .forEach((b) => b.classList.remove("active"));
        btn.classList.add("active");
    });
});


// header profile
document.addEventListener('DOMContentLoaded', function () {
    const profileDropdown = document.getElementById('profileDropdown');
    const dropdownMenu = document.getElementById('dropdownMenu');

    if (profileDropdown) {
        profileDropdown.addEventListener('click', function (event) {
            event.stopPropagation(); // Ngăn sự kiện click lan ra ngoài
            dropdownMenu.classList.toggle('show');
        });
    }

    // Đóng dropdown khi click ra ngoài
    window.addEventListener('click', function (event) {
        if (dropdownMenu && dropdownMenu.classList.contains('show')) {
            dropdownMenu.classList.remove('show');
        }
    });
});

// sidebar profile
document.addEventListener('DOMContentLoaded', function () {
    const sidebarProfile = document.getElementById('sidebarProfile');
    const sidebarDropdownMenu = document.getElementById('sidebarDropdownMenu');

    if (sidebarProfile) {
        sidebarProfile.addEventListener('click', function (event) {
            event.stopPropagation();
            sidebarDropdownMenu.classList.toggle('show');
        });
    }

    // Đóng dropdown khi click ra ngoài
    window.addEventListener('click', function (event) {
        if (sidebarDropdownMenu && sidebarDropdownMenu.classList.contains('show')) {
            sidebarDropdownMenu.classList.remove('show');
        }
    });
});



