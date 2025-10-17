package com.fptuni.vms.repository;

import com.fptuni.vms.model.SupportResponse;

import java.util.List;

public interface SupportResponseRepository {
    List<SupportResponse> findAllWithPagination(int page, int size);
    List<SupportResponse> findAll();
    SupportResponse save(SupportResponse response);
    List<SupportResponse> findByTicketId(Integer ticketId);
    boolean hasResponses(Integer ticketId);
    List<SupportResponse> filterResponses(String keyword, Integer num, int page, int size);
    long countFilteredResponses(String keyword);
}
