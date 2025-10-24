package com.fptuni.vms.service.impl;

import com.fptuni.vms.dto.response.AttendanceRecordDTO;
import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Attendance;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.repository.AttendanceRepository;
import com.fptuni.vms.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceServiceImpl implements AttendanceService {
    @Autowired
    private AttendanceRepository attendanceRepository;

    @Override
    public List<Opportunity> filterOpportunities(long orgId, String status, String keyword, String timeOrder, int page, int size) {
        return attendanceRepository.filterOpportunitiesByOrg(orgId, status, keyword, timeOrder, page, size);
    }

    @Override
    public long countFilteredOpportunities(long orgId, String status, String keyword) {
        return attendanceRepository.countOppAfterFilteredByOrg(orgId, status, keyword);
    }

    @Override
    public Opportunity getOpportunity(long id) {
        return attendanceRepository.getOpportunity(id);
    }

    @Override
    public Organization findOrganizationByOwnerId(Integer id) {
        return attendanceRepository.findOrganizationByOwnerId(id);
    }

    @Override
    public List<AttendanceRecordDTO> getAllAttendance(Integer opportunityId) {
        return attendanceRepository.getAllAttendanceRecordsForOpportunity(opportunityId);
    }

    @Override
    public List<AttendanceRecordDTO> getAttendanceListForOpportunity(Integer opportunityId, String keyword, String status, int page, int size) {
        return attendanceRepository.findAttendanceRecords(opportunityId, keyword, status, page, size);
    }

    @Override
    public long countFilteredAttendanceRecords(Integer opportunityId, String keyword, String status) {
        return attendanceRepository.countAttendanceRecords(opportunityId, keyword, status);
    }

    @Override
    public Application findApplicationById(Integer id) {
        return attendanceRepository.findApplicationById(id);
    }

    @Override
    public Attendance findAttendanceByApplicationId(Integer applicationId) {
        return attendanceRepository.findAttendanceByApplicationId(applicationId);
    }

    @Override
    public void createAttendance(Attendance attendance) {
        attendanceRepository.createAttendance(attendance);
    }

    @Override
    public void updateAttendance(Attendance attendance) {
        attendanceRepository.updateAttendance(attendance);
    }

    @Override
    @Transactional
    public void processAbsentVolunteers(List<AttendanceRecordDTO> attendanceList) {
        for (AttendanceRecordDTO dto : attendanceList) {
            if (dto.getCheckinTime() == null && dto.getCheckoutTime() == null) {
                Attendance attendance = findAttendanceByApplicationId(dto.getApplicationId());

                if (attendance == null) {
                    Application application = findApplicationById(dto.getApplicationId());
                    if (application != null) {
                        Attendance newAttendance = new Attendance();
                        newAttendance.setApplication(application);
                        newAttendance.setStatus(Attendance.AttendanceStatus.ABSENT);
                        createAttendance(newAttendance);
                    }
                } else {
                    attendance.setStatus(Attendance.AttendanceStatus.ABSENT);
                    updateAttendance(attendance);
                }
            }
        }
    }

    @Override
    public Map<String, Integer> getTotalStatusOfVolunteer(List<AttendanceRecordDTO> attendanceList) {
        Map<String, Integer> stats = new java.util.HashMap<>();
        int presentCount = 0;
        int absentCount = 0;

        if (attendanceList == null || attendanceList.isEmpty()) {
            stats.put("presentCount", 0);
            stats.put("absentCount", 0);
            return stats;
        }

        for (AttendanceRecordDTO dto : attendanceList) {
            String status = dto.getStatus();
            if ("PRESENT".equalsIgnoreCase(status)){
                presentCount++;
            } else if ("ABSENT".equalsIgnoreCase(status)){
                absentCount++;
            }
        }
        stats.put("presentCount", presentCount);
        stats.put("absentCount", absentCount);
        return stats;
    }

    @Override
    public BigDecimal getTotalHoursOfVolunteers(List<AttendanceRecordDTO> attendanceList) {
        BigDecimal totalHours = BigDecimal.ZERO;
        if (attendanceList == null || attendanceList.isEmpty()) {
            return totalHours;
        }
        for (AttendanceRecordDTO dto : attendanceList) {
            if (dto.getTotalHours() != null) {
                totalHours = totalHours.add(dto.getTotalHours());
            }
        }
        return totalHours;
    }


    @Override
    public double getCompleteStatistic(List<AttendanceRecordDTO> attendanceList) {
        if (attendanceList == null || attendanceList.isEmpty()) {
          return 0.0;
        }

        long completedCount = 0;
        for (AttendanceRecordDTO record : attendanceList) {
            if ("COMPLETED".equalsIgnoreCase(record.getStatus())) {
                completedCount++;
            }
        }

        int totalCount = attendanceList.size();
        return ((double) completedCount / totalCount) * 100.0;
    }

}
