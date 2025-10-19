package com.fptuni.vms.service;

import com.fptuni.vms.model.SupportResponse;
import com.fptuni.vms.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SupportResponseService {
    List<SupportResponse> getListResponseWithPagination(int page, int size);
    List<SupportResponse> getAllResponses();
    SupportResponse addResponse(SupportResponse response);
    List<SupportResponse> getResponsesByTicketId(Integer ticketId);
    boolean ticketHasResponses(Integer ticketId);
    List<SupportResponse> filterAndPaginateResponses(String keyword, Integer num, int page, int size);
    long countFilteredResponses(String keyword);
    boolean addAdminResponse(int ticketId, String message, User admin, MultipartFile attachment);
}
