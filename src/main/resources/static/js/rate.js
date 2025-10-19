document.addEventListener("DOMContentLoaded", () => {
    // === STAR RATING ===
    document.querySelectorAll('.stars').forEach(starGroup => {
        const labels = starGroup.querySelectorAll('label');

        labels.forEach((label, index) => {
            const input = label.querySelector('input[type="radio"]');
            const span = label.querySelector('span');

            // Khi click vào span hoặc label
            label.addEventListener('click', e => {
                e.preventDefault(); // tránh trigger form submit
                input.checked = true;
                updateStars(starGroup, index + 1);
            });
        });

        // Nếu có sao được chọn sẵn (trang chỉnh sửa)
        const checked = starGroup.querySelector('input[type="radio"]:checked');
        if (checked) {
            const idx = Array.from(labels).findIndex(l => l.querySelector('input').checked);
            updateStars(starGroup, idx + 1);
        }
    });

    // Cập nhật hiển thị sao
    function updateStars(group, value) {
        const spans = group.querySelectorAll('span');
        spans.forEach((s, i) => {
            s.textContent = i < value ? '★' : '☆';
            s.style.color = i < value ? '#f59e0b' : '#ccc';
        });
    }

    // === COMMENT COUNTER ===
    const textarea = document.querySelector('textarea[name$="comment"]');
    const counter = document.querySelector('.counter');
    if (textarea && counter) {
        const updateCount = () => counter.textContent = `Tối đa 500 ký tự (${textarea.value.length}/500)`;
        updateCount();
        textarea.addEventListener('input', updateCount);
    }
});

// === PAGE SIZE CHANGE ===
function changePageSize(size) {
    const params = new URLSearchParams(window.location.search);
    params.set('size', size);
    params.set('page', 0);
    window.location.search = params.toString();
}