package com.ta.managementproject.service;

import com.ta.managementproject.dto.BaseResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.awt.print.Pageable;
import java.util.Date;

@Component
public class UtilService {
    public <T> ResponseEntity<BaseResponseDTO<T>> buildResponse(
            HttpStatus status, String message, T data) {

        BaseResponseDTO<T> res = new BaseResponseDTO<>();
        res.setStatus(status.value());
        res.setTimestamp(new Date());
        res.setMessage(message);
        res.setData(data);

        return new ResponseEntity<>(res, status);
    }

    public Pageable createPagination(String sortingColumn, String orderDirection){
        return null;
    }
}
