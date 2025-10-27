package com.fptuni.vms.repository;

import com.fptuni.vms.dto.response.AttendanceRecordDTO;
import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Attendance;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;

import java.util.List;

public interface AttendanceRepository {

    // Attendance
    List<Opportunity> filterOpportunitiesByOrg(long orgId, String status, String keyword, String timeOrder, int page, int size);
    long countOppAfterFilteredByOrg(long orgId, String status, String keyword);
    Opportunity getOpportunity(long id);
    Organization findOrganizationByOwnerId(Integer id);


    // Attendance Details
    List<AttendanceRecordDTO> getAllAttendanceRecordsForOpportunity(Integer opportunityId);
    List<AttendanceRecordDTO> findAttendanceRecords(Integer opportunityId, String keyword, String status, int page, int size);
    long countAttendanceRecords(Integer opportunityId, String keyword, String status);
    Application findApplicationById(Integer id);
    Attendance findAttendanceByApplicationId(Integer applicationId);
    Attendance createAttendance(Attendance attendance);
    Attendance updateAttendance(Attendance attendance);


}