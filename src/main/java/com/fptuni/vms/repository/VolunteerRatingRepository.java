package com.fptuni.vms.repository;

import com.fptuni.vms.model.VolunteerRating;
import java.util.List;

public interface VolunteerRatingRepository {

    // Thêm / sửa
    void save(VolunteerRating rating);

    // Tính tổng số rating
    long countAll();

    // Lọc theo số sao (ví dụ 5 sao)
    long countByStars(short stars);

    // Lấy danh sách rating theo số sao
    List<VolunteerRating> findByStars(short stars);
}
