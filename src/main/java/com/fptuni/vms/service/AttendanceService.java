package com.fptuni.vms.service;

import com.fptuni.vms.dto.response.AttendanceRecordDTO;
import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Attendance;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    //attendance page related methods
    List<Opportunity> filterOpportunities(long orgId, String status, String keyword, String timeOrder, int page, int size);
    long countFilteredOpportunities(long orgId, String status, String keyword);
    Opportunity getOpportunity(long id);
    Organization findOrganizationByOwnerId(Integer id);

    //attendance detail page related methods
    List<AttendanceRecordDTO>getAllAttendance(Integer opportunityId);
    List<AttendanceRecordDTO> getAttendanceListForOpportunity(Integer opportunityId, String keyword, String status, int page, int size);
    long countFilteredAttendanceRecords(Integer opportunityId, String keyword, String status);
    Application findApplicationById(Integer id);
    Attendance findAttendanceByApplicationId(Integer applicationId);
    void createAttendance(Attendance attendance);
    void updateAttendance(Attendance attendance);
    void processAbsentVolunteers(List<AttendanceRecordDTO> attendanceList);
    Map<String, Integer> getTotalStatusOfVolunteer(List<AttendanceRecordDTO> attendanceList);
    BigDecimal getTotalHoursOfVolunteers(List<AttendanceRecordDTO> attendanceList);
    double getCompleteStatistic(List<AttendanceRecordDTO> attendanceList);

}
